package com.ban.vehicle_management.application.ai.service;

import java.time.Duration;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Distributed circuit-breaker policy for AI providers. The breaker is provider
 * agnostic; only transport/rate-limit failures feed the availability circuit.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "app.ai.circuit-breaker")
public class AiCircuitBreakerProperties {

    private int failureThreshold = 3;
    private Duration failureWindow = Duration.ofSeconds(120);
    private Duration openDuration = Duration.ofSeconds(60);
    private Duration minOpenDuration = Duration.ofSeconds(30);
    private Duration maxOpenDuration = Duration.ofSeconds(300);
    private Duration halfOpenLockTtl = Duration.ofSeconds(30);
}
