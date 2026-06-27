package com.ban.vehicle_management.infrastructure.storage;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@Getter
@Setter
@ConfigurationProperties(prefix = "app.storage.minio")
public class MinioStorageProperties {
    private String endpoint;
    private String accessKey;
    private String secretKey;
    private String publicBucket;
    private String privateBucket;
    private int presignedUrlExpireSeconds = 900;
    private String publicUrlBase;
    private long imageMaxSizeBytes = 5 * 1024 * 1024;
    private int imageMaxWidthPixels = 1280;
    private double imageJpegQuality = 0.85;
}
