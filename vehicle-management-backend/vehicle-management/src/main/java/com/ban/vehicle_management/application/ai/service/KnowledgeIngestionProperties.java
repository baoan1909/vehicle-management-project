package com.ban.vehicle_management.application.ai.service;

import jakarta.validation.constraints.AssertTrue;
import java.time.Duration;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@Getter
@Setter
@ConfigurationProperties(prefix = "app.ai.ingestion")
public class KnowledgeIngestionProperties {

    private boolean enabled = true;
    private long pollFixedDelayMs = 5000;
    private long pollInitialDelayMs = 15000;
    private int claimLimit = 3;
    private String workerIdPrefix = "kd-worker";
    private Duration lockDuration = Duration.ofMinutes(5);
    private Duration retryInitialDelay = Duration.ofMinutes(1);
    private Duration retryMaxDelay = Duration.ofMinutes(30);
    private int maxAttempts = 3;

    @AssertTrue(message = "AI ingestion retry max delay must be >= initial delay and durations must be positive")
    public boolean areDurationsValid() {
        return isPositive(lockDuration)
                && isPositive(retryInitialDelay)
                && isPositive(retryMaxDelay)
                && retryMaxDelay.compareTo(retryInitialDelay) >= 0;
    }

    private boolean isPositive(Duration value) {
        return value != null && !value.isZero() && !value.isNegative();
    }
}
