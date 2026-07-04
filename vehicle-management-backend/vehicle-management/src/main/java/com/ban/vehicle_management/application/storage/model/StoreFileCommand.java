package com.ban.vehicle_management.application.storage.model;

import com.ban.vehicle_management.shared.enumeration.storage.StorageBucket;
import com.ban.vehicle_management.shared.enumeration.storage.StorageFolder;
import java.util.Map;
import java.util.UUID;
import org.springframework.web.multipart.MultipartFile;

public record StoreFileCommand(
        MultipartFile file,
        StorageBucket bucket,
        StorageFolder folder,
        String resourceType,
        UUID resourceId,
        UUID ownerAccountId,
        Map<String, String> metadata
) {
    public Map<String, String> metadata() {
        return metadata == null ? Map.of() : Map.copyOf(metadata);
    }
}
