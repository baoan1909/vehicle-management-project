package com.ban.vehicle_management.application.ai.service;

import java.time.Duration;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

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
    private int maxAttempts = 3;
}