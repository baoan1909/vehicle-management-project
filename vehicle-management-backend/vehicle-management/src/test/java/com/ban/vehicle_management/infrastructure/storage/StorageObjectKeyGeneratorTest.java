package com.ban.vehicle_management.infrastructure.storage;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ban.vehicle_management.application.storage.model.StoreFileCommand;
import com.ban.vehicle_management.shared.enumeration.storage.StorageBucket;
import com.ban.vehicle_management.shared.enumeration.storage.StorageFolder;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class StorageObjectKeyGeneratorTest {

    private final StorageObjectKeyGenerator generator = new StorageObjectKeyGenerator();

    @Test
    void shouldGenerateShortAvatarObjectKeyWithBucketPrefixAndUuid() {
        UUID resourceId = UUID.randomUUID();

        String objectKey = generator.generate(new StoreFileCommand(
                null,
                StorageBucket.PUBLIC,
                StorageFolder.AVATAR,
                "people.user_profiles",
                resourceId,
                UUID.randomUUID(),
                Map.of()
        ), "JPG");

        assertTrue(objectKey.startsWith("av/"));
        assertTrue(objectKey.contains("/" + resourceId + "/pb-"));
        assertTrue(objectKey.endsWith("-avatar.jpg"));
        assertTrue(objectKey.length() <= 255);
    }
}
