package com.ban.vehicle_management.application.storage.config;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
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
@ConfigurationProperties(prefix = "app.storage.access")
public class StorageAccessTimeProperties {

    @NotNull
    private Duration parkingImageReadUrlExpiry = Duration.ofMinutes(15);

    @NotNull
    private Duration chatAttachmentReadUrlExpiry = Duration.ofMinutes(15);

    @AssertTrue(message = "Storage access URL expiry values must be greater than zero")
    public boolean areExpiriesValid() {
        return isPositive(parkingImageReadUrlExpiry) && isPositive(chatAttachmentReadUrlExpiry);
    }

    public int chatAttachmentReadUrlExpirySeconds() {
        return Math.toIntExact(chatAttachmentReadUrlExpiry.toSeconds());
    }

    public int parkingImageReadUrlExpirySeconds() {
        return Math.toIntExact(parkingImageReadUrlExpiry.toSeconds());
    }

    private boolean isPositive(Duration value) {
        return value != null && !value.isZero() && !value.isNegative();
    }
}
