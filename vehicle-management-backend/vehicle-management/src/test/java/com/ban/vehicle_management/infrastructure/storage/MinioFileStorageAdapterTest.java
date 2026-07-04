package com.ban.vehicle_management.infrastructure.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.ban.vehicle_management.application.storage.model.StoreFileCommand;
import com.ban.vehicle_management.application.storage.model.StoredFile;
import com.ban.vehicle_management.shared.enumeration.storage.StorageBucket;
import com.ban.vehicle_management.shared.enumeration.storage.StorageFolder;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.SetBucketPolicyArgs;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

@ExtendWith(MockitoExtension.class)
class MinioFileStorageAdapterTest {

    @Mock
    private MinioClient minioClient;

    @Mock
    private StorageObjectKeyGenerator objectKeyGenerator;

    @Mock
    private ImageFileProcessor imageFileProcessor;

    private MinioFileStorageAdapter adapter;

    @BeforeEach
    void setUp() {
        MinioStorageProperties properties = new MinioStorageProperties();
        properties.setPublicBucket("vehicle-public");
        properties.setPrivateBucket("vehicle-private");
        properties.setPresignedUrlExpireSeconds(900);
        adapter = new MinioFileStorageAdapter(minioClient, properties, objectKeyGenerator, imageFileProcessor);
    }

    @Test
    void shouldCreateBucketAndUploadPreparedFile() throws Exception {
        UUID resourceId = UUID.randomUUID();
        String objectKey = "av/2026/06/11/" + resourceId + "/pb-new-avatar.jpg";
        MockMultipartFile file = new MockMultipartFile("file", "avatar.jpg", "image/jpeg", new byte[]{1, 2, 3});
        StoreFileCommand command = new StoreFileCommand(
                file,
                StorageBucket.PUBLIC,
                StorageFolder.AVATAR,
                "people.user_profiles",
                resourceId,
                UUID.randomUUID(),
                Map.of()
        );

        when(imageFileProcessor.prepare(file)).thenReturn(new ImageFileProcessor.PreparedFile(
                new byte[]{1, 2, 3},
                "avatar.jpg",
                "image/jpeg",
                "jpg",
                "checksum"
        ));
        when(objectKeyGenerator.generate(command, "jpg")).thenReturn(objectKey);
        when(minioClient.bucketExists(any(BucketExistsArgs.class))).thenReturn(false);

        StoredFile storedFile = adapter.store(command);

        verify(minioClient).makeBucket(any(MakeBucketArgs.class));
        verify(minioClient).setBucketPolicy(any(SetBucketPolicyArgs.class));
        verify(minioClient).putObject(any(PutObjectArgs.class));
        assertEquals(objectKey, storedFile.objectKey());
        assertEquals("image/jpeg", storedFile.contentType());
        assertEquals(3, storedFile.sizeBytes());
    }

    @Test
    void shouldReturnConfiguredPublicUrlForPublicObjectWithoutPresigning() {
        MinioStorageProperties properties = new MinioStorageProperties();
        properties.setPublicBucket("vehicle-public");
        properties.setPrivateBucket("vehicle-private");
        properties.setPublicUrlBase("https://cdn.example.com/files/");
        MinioFileStorageAdapter publicAdapter = new MinioFileStorageAdapter(
                minioClient,
                properties,
                objectKeyGenerator,
                imageFileProcessor
        );

        String url = publicAdapter.createReadUrl("av/2026/06/11/profile/pb-new avatar.jpg", 60);

        assertEquals("https://cdn.example.com/files/av/2026/06/11/profile/pb-new%20avatar.jpg", url);
        verifyNoInteractions(minioClient);
    }
}
