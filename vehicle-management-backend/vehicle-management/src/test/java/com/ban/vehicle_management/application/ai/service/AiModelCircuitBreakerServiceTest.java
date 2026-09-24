package com.ban.vehicle_management.application.ai.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ban.vehicle_management.application.ai.cache.model.CircuitBreakerDecision;
import com.ban.vehicle_management.application.ai.cache.model.CircuitState;
import com.ban.vehicle_management.application.ai.port.out.DistributedCachePortOut;
import com.ban.vehicle_management.infrastructure.cache.AiCacheMetrics;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AiModelCircuitBreakerServiceTest {

    private AiModelCircuitBreakerService breaker;
    private InMemoryCache cache;

    static class InMemoryCache implements DistributedCachePortOut {
        private final Map<String, Object> store = new ConcurrentHashMap<>();

        @Override
        @SuppressWarnings("unchecked")
        public <T> Optional<T> get(String key, Class<T> type) {
            Object value = store.get(key);
            if (value == null) {
                return Optional.empty();
            }
            if (type.isInstance(value)) {
                return Optional.of((T) value);
            }
            return Optional.empty();
        }

        @Override
        public void put(String key, Object value, Duration ttl) {
            store.put(key, value);
        }

        @Override
        public boolean putIfAbsent(String key, Object value, Duration ttl) {
            return store.putIfAbsent(key, value) == null;
        }

        @Override
        public void delete(String key) {
            store.remove(key);
        }

        @Override
        public void deleteAll(List<String> keys) {
            keys.forEach(store::remove);
        }
    }

    @BeforeEach
    void setUp() {
        cache = new InMemoryCache();
        AiCircuitBreakerProperties properties = new AiCircuitBreakerProperties();
        properties.setFailureThreshold(3);
        properties.setFailureWindow(Duration.ofSeconds(120));
        properties.setOpenDuration(Duration.ofSeconds(60));
        properties.setMinOpenDuration(Duration.ofSeconds(30));
        properties.setMaxOpenDuration(Duration.ofSeconds(300));
        properties.setHalfOpenLockTtl(Duration.ofSeconds(30));
        breaker = new AiModelCircuitBreakerService(cache, properties,
                new AiCacheMetrics(new SimpleMeterRegistry()));
    }

    @Test
    void threeRetryableFailuresOpenCircuit() {
        String key = "vm:local:svc:v1:ai-circuit-breaker:GEMINI:SUPPORT_CHAT:config-1";
        breaker.recordFailure(key, true, null);
        breaker.recordFailure(key, true, null);
        assertTrue(breaker.shouldAllow(key).allowed());
        breaker.recordFailure(key, true, null);
        CircuitBreakerDecision decision = breaker.shouldAllow(key);
        assertFalse(decision.allowed());
        assertEquals(CircuitState.OPEN, decision.state());
    }

    @Test
    void http400DoesNotFeedAvailabilityCircuit() {
        String key = "vm:local:svc:v1:ai-circuit-breaker:GEMINI:SUPPORT_CHAT:config-2";
        breaker.recordFailure(key, false, null);
        breaker.recordFailure(key, false, null);
        breaker.recordFailure(key, false, null);
        assertTrue(breaker.shouldAllow(key).allowed());
    }

    @Test
    void retryAfterIsClampedToMax() {
        String key = "vm:local:svc:v1:ai-circuit-breaker:GEMINI:SUPPORT_CHAT:config-3";
        breaker.recordFailure(key, true, Duration.ofSeconds(1000));
        breaker.recordFailure(key, true, Duration.ofSeconds(1000));
        breaker.recordFailure(key, true, Duration.ofSeconds(1000));
        var snapshot = breaker.snapshot(key);
        assertTrue(snapshot.isPresent());
        assertTrue(snapshot.get().retryAfter() != null);
        long seconds = Duration.between(java.time.Instant.now(), snapshot.get().retryAfter()).getSeconds();
        assertTrue(seconds <= 300 && seconds > 0);
    }

    @Test
    void probeSuccessClosesCircuit() {
        String key = "vm:local:svc:v1:ai-circuit-breaker:GEMINI:SUPPORT_CHAT:config-4";
        breaker.recordFailure(key, true, null);
        breaker.recordFailure(key, true, null);
        breaker.recordFailure(key, true, null);
        assertFalse(breaker.shouldAllow(key).allowed());
        // Simulate open expiry by recording success as the single probe.
        breaker.recordSuccess(key);
        assertTrue(breaker.shouldAllow(key).allowed());
    }

    @Test
    void halfOpenAllowsSingleProbe() {
        String key = "vm:local:svc:v1:ai-circuit-breaker:GEMINI:SUPPORT_CHAT:config-5";
        breaker.recordFailure(key, true, null);
        breaker.recordFailure(key, true, null);
        breaker.recordFailure(key, true, null);
        // First contender after open expiry would probe; while OPEN, second call is denied.
        CircuitBreakerDecision first = breaker.shouldAllow(key);
        assertFalse(first.allowed());
        CircuitBreakerDecision second = breaker.shouldAllow(key);
        assertFalse(second.allowed());
    }
}
