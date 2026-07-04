package com.ban.vehicle_management.infrastructure.storage;

import com.ban.vehicle_management.application.storage.model.StoreFileCommand;
import com.ban.vehicle_management.application.storage.model.StoredFile;
import com.ban.vehicle_management.application.storage.port.out.FileAccessPort;
import com.ban.vehicle_management.application.storage.port.out.FileStoragePort;
import com.ban.vehicle_management.shared.enumeration.storage.StorageBucket;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import io.minio.BucketExistsArgs;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.SetBucketPolicyArgs;
import io.minio.StatObjectArgs;
import io.minio.http.Method;
import java.io.ByteArrayInputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class MinioFileStorageAdapter implements FileStoragePort, FileAccessPort {

    private final MinioClient minioClient;
    private final MinioStorageProperties properties;
    private final StorageObjectKeyGenerator objectKeyGenerator;
    private final ImageFileProcessor imageFileProcessor;

    public MinioFileStorageAdapter(
            MinioClient minioClient,
            MinioStorageProperties properties,
            StorageObjectKeyGenerator objectKeyGenerator,
            ImageFileProcessor imageFileProcessor
    ) {
        this.minioClient = minioClient;
        this.properties = properties;
        this.objectKeyGenerator = objectKeyGenerator;
        this.imageFileProcessor = imageFileProcessor;
    }

    @Override
    public StoredFile store(StoreFileCommand command) {
        validateStoreCommand(command);
        ImageFileProcessor.PreparedFile preparedFile = imageFileProcessor.prepare(command.file());
        String bucketName = bucketName(command.bucket());
        String objectKey = objectKeyGenerator.generate(command, preparedFile.extension());

        try {
            ensureBucketExists(bucketName, command.bucket());
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectKey)
                    .stream(new ByteArrayInputStream(preparedFile.bytes()), preparedFile.bytes().length, -1)
                    .contentType(preparedFile.contentType())
                    .userMetadata(buildMetadata(command, preparedFile))
                    .build());
            return new StoredFile(
                    objectKey,
                    preparedFile.originalFilename(),
                    preparedFile.contentType(),
                    preparedFile.bytes().length,
                    preparedFile.checksumSha256()
            );
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to store file in MinIO", exception);
        }
    }

    @Override
    public void delete(String objectKey) {
        String normalizedObjectKey = normalizeObjectKey(objectKey);
        if (normalizedObjectKey == null) {
            return;
        }
        try {
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(bucketName(resolveBucket(normalizedObjectKey)))
                    .object(normalizedObjectKey)
                    .build());
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to delete file from MinIO", exception);
        }
    }

    @Override
    public boolean exists(String objectKey) {
        String normalizedObjectKey = normalizeObjectKey(objectKey);
        if (normalizedObjectKey == null) {
            return false;
        }
        try {
            minioClient.statObject(StatObjectArgs.builder()
                    .bucket(bucketName(resolveBucket(normalizedObjectKey)))
                    .object(normalizedObjectKey)
                    .build());
            return true;
        } catch (Exception exception) {
            return false;
        }
    }

    @Override
    public Optional<String> createPublicUrl(String objectKey) {
        String normalizedObjectKey = normalizeObjectKey(objectKey);
        if (normalizedObjectKey == null) {
            return Optional.empty();
        }
        try {
            if (resolveBucket(normalizedObjectKey) != StorageBucket.PUBLIC) {
                return Optional.empty();
            }
        } catch (BadRequestException exception) {
            return Optional.empty();
        }
        return Optional.ofNullable(createConfiguredPublicUrl(normalizedObjectKey));
    }

    @Override
    public String createReadUrl(String objectKey, int expireSeconds) {
        String normalizedObjectKey = requireObjectKey(objectKey);
        StorageBucket bucket = resolveBucket(normalizedObjectKey);
        Optional<String> publicUrl = createPublicUrl(normalizedObjectKey);
        if (publicUrl.isPresent()) {
            return publicUrl.get();
        }

        int resolvedExpireSeconds = expireSeconds > 0
                ? expireSeconds
                : properties.getPresignedUrlExpireSeconds();
        try {
            return minioClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                    .method(Method.GET)
                    .bucket(bucketName(bucket))
                    .object(normalizedObjectKey)
                    .expiry(resolvedExpireSeconds)
                    .build());
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to create file read URL", exception);
        }
    }

    @Override
    public Map<String, String> createReadUrls(Set<String> objectKeys, int expireSeconds) {
        if (objectKeys == null || objectKeys.isEmpty()) {
            return Map.of();
        }
        return objectKeys.stream()
                .filter(objectKey -> normalizeObjectKey(objectKey) != null)
                .collect(Collectors.toMap(objectKey -> objectKey, objectKey -> createReadUrl(objectKey, expireSeconds)));
    }

    private void validateStoreCommand(StoreFileCommand command) {
        if (command == null) {
            throw new BadRequestException("storeFileCommand must not be null");
        }
        if (command.bucket() == null) {
            throw new BadRequestException("storage bucket must not be null");
        }
        if (command.folder() == null) {
            throw new BadRequestException("storage folder must not be null");
        }
        if (command.resourceType() == null || command.resourceType().isBlank()) {
            throw new BadRequestException("resourceType must not be blank");
        }
        if (command.resourceId() == null) {
            throw new BadRequestException("resourceId must not be null");
        }
    }

    private void ensureBucketExists(String bucketName, StorageBucket bucket) throws Exception {
        boolean bucketExists = minioClient.bucketExists(BucketExistsArgs.builder()
                .bucket(bucketName)
                .build());
        if (!bucketExists) {
            minioClient.makeBucket(MakeBucketArgs.builder()
                    .bucket(bucketName)
                    .build());
            if (bucket == StorageBucket.PUBLIC) {
                minioClient.setBucketPolicy(SetBucketPolicyArgs.builder()
                        .bucket(bucketName)
                        .config(publicReadPolicy(bucketName))
                        .build());
            }
        }
    }

    private Map<String, String> buildMetadata(
            StoreFileCommand command,
            ImageFileProcessor.PreparedFile preparedFile
    ) {
        Map<String, String> metadata = new HashMap<>();
        metadata.put("resource_type", command.resourceType());
        metadata.put("resource_id", command.resourceId().toString());
        metadata.put("checksum_sha256", preparedFile.checksumSha256());
        metadata.put("content_type", preparedFile.contentType());
        metadata.put("size_bytes", String.valueOf(preparedFile.bytes().length));
        metadata.put("original_filename", encodeMetadataValue(preparedFile.originalFilename()));
        if (command.ownerAccountId() != null) {
            metadata.put("owner_account_id", command.ownerAccountId().toString());
        }
        command.metadata().forEach((key, value) -> {
            if (key != null && !key.isBlank() && value != null && !value.isBlank()) {
                metadata.put(key, value);
            }
        });
        return metadata;
    }

    private String publicReadPolicy(String bucketName) {
        return """
                {
                  "Version": "2012-10-17",
                  "Statement": [
                    {
                      "Effect": "Allow",
                      "Principal": {"AWS": ["*"]},
                      "Action": ["s3:GetObject"],
                      "Resource": ["arn:aws:s3:::%s/*"]
                    }
                  ]
                }
                """.formatted(bucketName);
    }

    private String createConfiguredPublicUrl(String objectKey) {
        String publicUrlBase = properties.getPublicUrlBase();
        if (publicUrlBase == null || publicUrlBase.isBlank()) {
            return null;
        }
        return publicUrlBase.replaceFirst("/+$", "") + "/" + encodeObjectKeyPath(objectKey);
    }

    private String encodeObjectKeyPath(String objectKey) {
        return Arrays.stream(objectKey.split("/", -1))
                .map(this::encodeUrlSegment)
                .collect(Collectors.joining("/"));
    }

    private String encodeUrlSegment(String segment) {
        return URLEncoder.encode(segment, StandardCharsets.UTF_8).replace("+", "%20");
    }

    private String encodeMetadataValue(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }

    private String bucketName(StorageBucket bucket) {
        return switch (bucket) {
            case PUBLIC -> properties.getPublicBucket();
            case PRIVATE -> properties.getPrivateBucket();
        };
    }

    private StorageBucket resolveBucket(String objectKey) {
        if (objectKey.contains("/" + StorageBucket.PUBLIC.objectKeyPrefix() + "-")) {
            return StorageBucket.PUBLIC;
        }
        if (objectKey.contains("/" + StorageBucket.PRIVATE.objectKeyPrefix() + "-")) {
            return StorageBucket.PRIVATE;
        }
        throw new BadRequestException("objectKey has an unsupported bucket prefix");
    }

    private String requireObjectKey(String objectKey) {
        String normalizedObjectKey = normalizeObjectKey(objectKey);
        if (normalizedObjectKey == null) {
            throw new BadRequestException("objectKey must not be blank");
        }
        return normalizedObjectKey;
    }

    private String normalizeObjectKey(String objectKey) {
        if (objectKey == null) {
            return null;
        }
        String normalizedObjectKey = objectKey.trim();
        return normalizedObjectKey.isBlank() ? null : normalizedObjectKey;
    }
}
