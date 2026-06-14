package com.ban.vehicle_management.application.storage.port.out;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

public interface FileAccessPort {

    Optional<String> createPublicUrl(String objectKey);

    String createReadUrl(String objectKey, int expireSeconds);

    Map<String, String> createReadUrls(Set<String> objectKeys, int expireSeconds);
}
