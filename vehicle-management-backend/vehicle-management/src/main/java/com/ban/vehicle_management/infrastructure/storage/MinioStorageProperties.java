package com.ban.vehicle_management.infrastructure.storage;

import jakarta.validation.constraints.AssertTrue;
import java.time.Duration;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Component
@Validated
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
    private Duration presignedUrlExpiry = Duration.ofMinutes(15);
    private String publicUrlBase;
    private long imageMaxSizeBytes = 5 * 1024 * 1024;
    private int imageMaxWidthPixels = 1280;
    private double imageJpegQuality = 0.85;

    @AssertTrue(message = "MinIO presigned URL expiry must be greater than zero")
    public boolean isPresignedUrlExpiryValid() {
        return presignedUrlExpiry != null && !presignedUrlExpiry.isZero() && !presignedUrlExpiry.isNegative();
    }
}
