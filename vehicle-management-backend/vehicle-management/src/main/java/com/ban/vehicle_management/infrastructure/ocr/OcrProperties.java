package com.ban.vehicle_management.infrastructure.ocr;

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
@ConfigurationProperties(prefix = "app.ocr")
public class OcrProperties {
    private boolean enabled = true;
    private String baseUrl = "http://localhost:8010";
    private String internalToken = "dev-ocr-internal-token";
    private int connectTimeoutMs = 1500;
    private int readTimeoutMs = 10000;
    private Duration connectTimeout = Duration.ofMillis(1500);
    private Duration readTimeout = Duration.ofSeconds(10);
    private double confidenceThreshold = 0.70;

    @AssertTrue(message = "OCR connect/read timeouts must be greater than zero")
    public boolean areTimeoutsValid() {
        return isPositive(connectTimeout) && isPositive(readTimeout);
    }

    private boolean isPositive(Duration value) {
        return value != null && !value.isZero() && !value.isNegative();
    }
}
