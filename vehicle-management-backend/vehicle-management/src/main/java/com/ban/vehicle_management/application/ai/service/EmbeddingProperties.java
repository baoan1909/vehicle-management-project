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
@ConfigurationProperties(prefix = "app.ai.embedding")
public class EmbeddingProperties {

    private boolean enabled;
    private boolean vectorSearchEnabled;
    private boolean lexicalFallbackEnabled = true;
    private int retryMaxAttempts = 3;
    private Duration retryInitialDelay = Duration.ofSeconds(1);
    private Duration retryMaxDelay = Duration.ofSeconds(10);
    private boolean retryJitterEnabled = true;
    private int buildBatchSize = 5;
    private long buildFixedDelayMs = 5000;
    private long buildInitialDelayMs = 10000;
    private Duration buildLeaseDuration = Duration.ofMinutes(10);
    private boolean pgvectorRequired;

    @AssertTrue(message = "AI embedding retry max delay must be >= initial delay and durations must be positive")
    public boolean areDurationsValid() {
        return isPositive(retryInitialDelay)
                && isPositive(retryMaxDelay)
                && isPositive(buildLeaseDuration)
                && retryMaxDelay.compareTo(retryInitialDelay) >= 0;
    }

    private boolean isPositive(Duration value) {
        return value != null && !value.isZero() && !value.isNegative();
    }
}
