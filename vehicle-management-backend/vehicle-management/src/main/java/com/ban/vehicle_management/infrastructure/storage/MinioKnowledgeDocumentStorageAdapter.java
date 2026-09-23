package com.ban.vehicle_management.infrastructure.storage;

import com.ban.vehicle_management.application.ai.port.out.KnowledgeDocumentStoragePort;
import com.ban.vehicle_management.shared.enumeration.storage.StorageBucket;
import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.StatObjectArgs;
import io.minio.StatObjectResponse;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Locale;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Stores knowledge documents in the private bucket. Object keys follow the same
 * layout as the shared storage key generator so they remain recognizable, but they
 * are built from raw bytes rather than a {@code MultipartFile}.
 */
@Component
public class MinioKnowledgeDocumentStorageAdapter implements KnowledgeDocumentStoragePort {

    private static final String FOLDER_SEGMENT = "kd";
    private static final String FILE_ROLE = "knowledge-document";

    private final MinioClient minioClient;
    private final MinioStorageProperties properties;

    public MinioKnowledgeDocumentStorageAdapter(MinioClient minioClient, MinioStorageProperties properties) {
        this.minioClient = minioClient;
        this.properties = properties;
    }

    @Override
    public String store(byte[] content, String contentType, UUID documentId, String extension) {
        String objectKey = buildObjectKey(documentId, extension);
        String bucketName = properties.getPrivateBucket();
        try {
            ensureBucketExists(bucketName);
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectKey)
                    .stream(new ByteArrayInputStream(content), content.length, -1)
                    .contentType(contentType)
                    .build());
            return objectKey;
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to store knowledge document in MinIO", exception);
        }
    }

    @Override
    public InputStream openRead(String objectKey) {
        try {
            return minioClient.getObject(GetObjectArgs.builder()
                    .bucket(properties.getPrivateBucket())
                    .object(objectKey)
                    .build());
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to open knowledge document from MinIO", exception);
        }
    }

    @Override
    public boolean exists(String objectKey) {
        try {
            minioClient.statObject(StatObjectArgs.builder()
                    .bucket(properties.getPrivateBucket())
                    .object(objectKey)
                    .build());
            return true;
        } catch (Exception exception) {
            return false;
        }
    }

    @Override
    public long size(String objectKey) {
        try {
            StatObjectResponse stat = minioClient.statObject(StatObjectArgs.builder()
                    .bucket(properties.getPrivateBucket())
                    .object(objectKey)
                    .build());
            return stat.size();
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to stat knowledge document in MinIO", exception);
        }
    }

    @Override
    public void delete(String objectKey) {
        if (objectKey == null || objectKey.isBlank()) {
            return;
        }
        try {
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(properties.getPrivateBucket())
                    .object(objectKey)
                    .build());
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to delete knowledge document from MinIO", exception);
        }
    }

    private void ensureBucketExists(String bucketName) throws Exception {
        boolean bucketExists = minioClient.bucketExists(BucketExistsArgs.builder()
                .bucket(bucketName)
                .build());
        if (!bucketExists) {
            minioClient.makeBucket(MakeBucketArgs.builder()
                    .bucket(bucketName)
                    .build());
        }
    }

    private String buildObjectKey(UUID documentId, String extension) {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        String normalizedExtension = extension == null || extension.isBlank()
                ? "bin"
                : extension.toLowerCase(Locale.ROOT).replaceFirst("^\\.", "");
        return "%s/%04d/%02d/%02d/%s/%s-%s-%s.%s".formatted(
                FOLDER_SEGMENT,
                today.getYear(),
                today.getMonthValue(),
                today.getDayOfMonth(),
                documentId,
                StorageBucket.PRIVATE.objectKeyPrefix(),
                UUID.randomUUID(),
                FILE_ROLE,
                normalizedExtension);
    }
}