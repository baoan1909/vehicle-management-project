package com.ban.vehicle_management.application.ai.service;

import jakarta.validation.constraints.AssertTrue;
import java.time.Duration;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Distributed circuit-breaker policy for AI providers. The breaker is provider
 * agnostic; only transport/rate-limit failures feed the availability circuit.
 */
@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "app.ai.circuit-breaker")
public class AiCircuitBreakerProperties {

    private int failureThreshold = 3;
    private Duration failureWindow = Duration.ofSeconds(120);
    private Duration openDuration = Duration.ofSeconds(60);
    private Duration minOpenDuration = Duration.ofSeconds(30);
    private Duration maxOpenDuration = Duration.ofSeconds(300);
    private Duration halfOpenLockTtl = Duration.ofSeconds(30);
    private Duration retentionMargin = Duration.ofMinutes(5);

    @AssertTrue(message = "AI circuit-breaker durations must be positive and min-open <= open <= max-open")
    public boolean isConfigurationValid() {
        return failureThreshold > 0
                && isPositive(failureWindow)
                && isPositive(openDuration)
                && isPositive(minOpenDuration)
                && isPositive(maxOpenDuration)
                && isPositive(halfOpenLockTtl)
                && isPositive(retentionMargin)
                && minOpenDuration.compareTo(openDuration) <= 0
                && openDuration.compareTo(maxOpenDuration) <= 0;
    }

    private boolean isPositive(Duration value) {
        return value != null && !value.isZero() && !value.isNegative();
    }
}
