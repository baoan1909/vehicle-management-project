package com.ban.vehicle_management.application.storage.service;

import com.ban.vehicle_management.application.storage.port.out.FileAccessPort;
import com.ban.vehicle_management.shared.enumeration.storage.StorageBucket;
import com.ban.vehicle_management.shared.enumeration.storage.StorageFolder;
import org.springframework.stereotype.Service;

@Service
public class StorageUrlResolver {

    private final FileAccessPort fileAccessPort;

    public StorageUrlResolver(FileAccessPort fileAccessPort) {
        this.fileAccessPort = fileAccessPort;
    }

    public String resolvePublicAvatarUrl(String avatarUrl) {
        if (!isManagedAvatarObjectKey(avatarUrl)) {
            return avatarUrl;
        }
        return fileAccessPort.createPublicUrl(avatarUrl).orElse(avatarUrl);
    }

    public boolean isManagedAvatarObjectKey(String objectKey) {
        return objectKey != null
                && objectKey.startsWith(StorageFolder.AVATAR.pathSegment() + "/")
                && objectKey.contains("/" + StorageBucket.PUBLIC.objectKeyPrefix() + "-");
    }
}
