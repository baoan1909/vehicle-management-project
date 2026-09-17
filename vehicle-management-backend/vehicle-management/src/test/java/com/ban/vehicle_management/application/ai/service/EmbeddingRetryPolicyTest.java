package com.ban.vehicle_management.application.ai.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class EmbeddingRetryPolicyTest {

    @Test
    void shouldExponentialBackoffWithJitterDisabled() {
        EmbeddingRetryPolicy policy = new EmbeddingRetryPolicy(
                Duration.ofSeconds(1),
                Duration.ofSeconds(10),
                false
        );
        assertEquals(1000L, policy.nextDelayMillis(0, null));
        assertEquals(2000L, policy.nextDelayMillis(1, null));
        assertEquals(4000L, policy.nextDelayMillis(2, null));
        assertEquals(10000L, policy.nextDelayMillis(4, null));
    }

    @Test
    void shouldHonorRetryAfterHeaderWithinMax() {
        EmbeddingRetryPolicy policy = new EmbeddingRetryPolicy(
                Duration.ofSeconds(1),
                Duration.ofSeconds(10),
                false
        );
        assertEquals(5000L, policy.nextDelayMillis(0, 5L));
        assertEquals(10000L, policy.nextDelayMillis(0, 60L));
    }

    @Test
    void shouldKeepDelayWithinBoundsWhenJitterEnabled() {
        EmbeddingRetryPolicy policy = new EmbeddingRetryPolicy(
                Duration.ofSeconds(1),
                Duration.ofSeconds(10),
                true
        );
        long delay = policy.nextDelayMillis(2, null);
        assertTrue(delay >= 0, "Delay must be non-negative");
        assertTrue(delay <= 10000L, "Delay must not exceed max");
    }
}