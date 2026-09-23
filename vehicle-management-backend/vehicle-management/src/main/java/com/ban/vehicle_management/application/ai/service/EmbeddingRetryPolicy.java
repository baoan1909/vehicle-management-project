package com.ban.vehicle_management.application.ai.service;

import java.time.Duration;
import java.util.Random;

/**
 * Pure retry delay policy: delay = min(maxDelay, initialDelay * 2^attempt) with optional
 * jitter. A provider-supplied Retry-After hint (seconds) takes precedence when present.
 */
public final class EmbeddingRetryPolicy {

    private final Duration initialDelay;
    private final Duration maxDelay;
    private final boolean jitterEnabled;

    public EmbeddingRetryPolicy(Duration initialDelay, Duration maxDelay, boolean jitterEnabled) {
        this.initialDelay = initialDelay == null ? Duration.ZERO : initialDelay;
        this.maxDelay = maxDelay == null ? Duration.ofSeconds(10) : maxDelay;
        this.jitterEnabled = jitterEnabled;
    }

    public long nextDelayMillis(int attempt, Long retryAfterSeconds) {
        if (retryAfterSeconds != null && retryAfterSeconds > 0L) {
            return Math.min(retryAfterSeconds * 1000L, maxDelay.toMillis());
        }
        long exponent = Math.min(Math.max(attempt, 0), 16L);
        long base = initialDelay.toMillis() * (1L << exponent);
        long capped = Math.min(base, maxDelay.toMillis());
        if (!jitterEnabled || capped <= 0) {
            return capped;
        }
        Random random = new Random();
        long jitterRange = Math.max(1L, capped / 2);
        return Math.min(capped / 2 + random.nextLong(jitterRange), maxDelay.toMillis());
    }
}