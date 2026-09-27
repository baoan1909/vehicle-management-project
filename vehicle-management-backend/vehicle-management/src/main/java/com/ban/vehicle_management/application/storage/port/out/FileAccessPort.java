package com.ban.vehicle_management.application.storage.port.out;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

public interface FileAccessPort {

    Optional<String> createPublicUrl(String objectKey);

    String createReadUrl(String objectKey, int expireSeconds);

    default String createReadUrl(String objectKey) {
        return createReadUrl(objectKey, 0);
    }

    Map<String, String> createReadUrls(Set<String> objectKeys, int expireSeconds);

    default Map<String, String> createReadUrls(Set<String> objectKeys) {
        return createReadUrls(objectKeys, 0);
    }
}
