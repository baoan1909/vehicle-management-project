package com.ban.vehicle_management.application.ai.service;

import java.time.Duration;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

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
}
