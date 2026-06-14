package com.ban.vehicle_management.application.storage.model;

public record StoredFile(
        String objectKey,
        String originalFilename,
        String contentType,
        long sizeBytes,
        String checksumSha256
) {
}
