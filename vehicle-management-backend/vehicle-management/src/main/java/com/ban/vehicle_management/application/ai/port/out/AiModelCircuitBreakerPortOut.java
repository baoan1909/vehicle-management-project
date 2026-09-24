package com.ban.vehicle_management.application.ai.port.out;

import com.ban.vehicle_management.application.ai.cache.model.CircuitBreakerDecision;
import com.ban.vehicle_management.application.ai.cache.model.CircuitBreakerSnapshot;
import java.time.Duration;
import java.util.Optional;

/**
 * Provider-agnostic model circuit breaker. Implementations must keep a local
 * in-memory fallback so a Redis outage never blocks the chatbot.
 */
public interface AiModelCircuitBreakerPortOut {

    CircuitBreakerDecision shouldAllow(String circuitKey);

    void recordSuccess(String circuitKey);

    void recordFailure(String circuitKey, boolean retryable, Duration retryAfter);

    void recordAuthFailure(String circuitKey);

    Optional<CircuitBreakerSnapshot> snapshot(String circuitKey);
}
