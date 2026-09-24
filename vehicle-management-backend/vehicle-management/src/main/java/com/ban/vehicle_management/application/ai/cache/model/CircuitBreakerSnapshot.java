package com.ban.vehicle_management.application.ai.cache.model;

import java.time.Instant;

public record CircuitBreakerSnapshot(CircuitState state, int failureCount, Instant openedAt, Instant retryAfter) {
}
