package com.ban.vehicle_management.application.ai.cache.model;

import java.time.Instant;

public record CircuitBreakerDecision(boolean allowed, CircuitState state, Instant retryAfter) {
}
