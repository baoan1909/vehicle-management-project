package com.ban.vehicle_management.application.ai.service;

import com.ban.vehicle_management.application.ai.cache.model.CircuitBreakerDecision;
import com.ban.vehicle_management.application.ai.cache.model.CircuitBreakerSnapshot;
import com.ban.vehicle_management.application.ai.cache.model.CircuitState;
import com.ban.vehicle_management.application.ai.port.out.AiModelCircuitBreakerPortOut;
import com.ban.vehicle_management.application.ai.port.out.DistributedCachePortOut;
import com.ban.vehicle_management.infrastructure.cache.AiCacheMetrics;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Redis-backed circuit breaker with local in-memory fallback. Redis state is
 * authoritative when reachable; local state only protects the instance while
 * Redis is degraded so Gemini is never hammered during an outage.
 */
@Component
public class AiModelCircuitBreakerService implements AiModelCircuitBreakerPortOut {

    private static final Logger log = LoggerFactory.getLogger(AiModelCircuitBreakerService.class);

    private final DistributedCachePortOut distributedCache;
    private final AiCircuitBreakerProperties breakerProperties;
    private final AiCacheMetrics metrics;
    private final Map<String, LocalCircuit> localCircuits = new ConcurrentHashMap<>();

    public AiModelCircuitBreakerService(DistributedCachePortOut distributedCache,
            AiCircuitBreakerProperties breakerProperties, AiCacheMetrics metrics) {
        this.distributedCache = distributedCache;
        this.breakerProperties = breakerProperties;
        this.metrics = metrics;
    }

    @Override
    public CircuitBreakerDecision shouldAllow(String circuitKey) {
        LocalCircuit local = localCircuits.computeIfAbsent(circuitKey, ignored -> new LocalCircuit());
        Optional<CircuitRecord> remote = readRemote(circuitKey);
        CircuitRecord effective = remote.orElse(local.toRecord());
        Instant now = Instant.now();
        Instant effectiveRetryAfter = parseInstant(effective.retryAfter());
        if (CircuitState.OPEN.name().equals(effective.state())) {
            if (effectiveRetryAfter != null && now.isBefore(effectiveRetryAfter)) {
                return new CircuitBreakerDecision(false, CircuitState.OPEN, effectiveRetryAfter);
            }
            // Transition to HALF_OPEN: only one probe may proceed (distributed).
            if (tryAcquireProbe(circuitKey, effective)) {
                metrics.circuitHalfOpen(providerOf(circuitKey), "support_chat");
                local.state = CircuitState.HALF_OPEN;
                return new CircuitBreakerDecision(true, CircuitState.HALF_OPEN, null);
            }
            return new CircuitBreakerDecision(false, CircuitState.OPEN, effectiveRetryAfter);
        }
        if (CircuitState.HALF_OPEN.name().equals(effective.state())) {
            return new CircuitBreakerDecision(false, CircuitState.HALF_OPEN, null);
        }
        return new CircuitBreakerDecision(true, CircuitState.CLOSED, null);
    }

    @Override
    public void recordSuccess(String circuitKey) {
        LocalCircuit local = localCircuits.computeIfAbsent(circuitKey, ignored -> new LocalCircuit());
        local.reset();
        writeRemote(circuitKey, new CircuitRecord(CircuitState.CLOSED.name(), 0, null, null, Instant.now().toString()));
        metrics.circuitClose(providerOf(circuitKey), "support_chat");
    }

    @Override
    public void recordFailure(String circuitKey, boolean retryable, Duration retryAfter) {
        if (!retryable) {
            return;
        }
        LocalCircuit local = localCircuits.computeIfAbsent(circuitKey, ignored -> new LocalCircuit());
        local.onFailure();
        try {
            CircuitRecord current = readRemote(circuitKey).orElse(new CircuitRecord(
                    CircuitState.CLOSED.name(), 0, null, null, Instant.now().toString()));
            int failures = current.failureCount() + 1;
            Instant windowStart = current.windowStart() == null ? Instant.now() : Instant.parse(current.windowStart());
            if (Duration.between(windowStart, Instant.now()).compareTo(window()) > 0) {
                failures = 1;
                windowStart = Instant.now();
            }
            if (failures >= threshold()) {
                Duration openDuration = clampOpen(retryAfter);
                Instant retryAt = Instant.now().plus(openDuration);
                CircuitRecord opened = new CircuitRecord(CircuitState.OPEN.name(), failures,
                        Instant.now().toString(), retryAt.toString(), windowStart.toString());
                writeRemote(circuitKey, opened);
                local.open(retryAt);
                metrics.circuitOpen(providerOf(circuitKey), "support_chat");
            } else {
                writeRemote(circuitKey, new CircuitRecord(CircuitState.CLOSED.name(), failures,
                        null, null, windowStart.toString()));
            }
        } catch (Exception exception) {
            log.debug("Circuit breaker record failed error={}", exception.getClass().getSimpleName());
            if (local.failureCount >= threshold()) {
                local.open(Instant.now().plus(clampOpen(retryAfter)));
            }
        }
    }

    @Override
    public void recordAuthFailure(String circuitKey) {
        LocalCircuit local = localCircuits.computeIfAbsent(circuitKey, ignored -> new LocalCircuit());
        Instant retryAt = Instant.now().plus(maxOpen());
        local.open(retryAt);
        writeRemote(circuitKey, new CircuitRecord(CircuitState.OPEN.name(), threshold(),
                Instant.now().toString(), retryAt.toString(), Instant.now().toString()));
        metrics.circuitOpen(providerOf(circuitKey), "support_chat");
    }

    @Override
    public Optional<CircuitBreakerSnapshot> snapshot(String circuitKey) {
        Optional<CircuitRecord> remote = readRemote(circuitKey);
        LocalCircuit local = localCircuits.get(circuitKey);
        CircuitRecord record = remote.orElse(local == null ? null : local.toRecord());
        if (record == null) {
            return Optional.empty();
        }
        try {
            CircuitState state = CircuitState.valueOf(record.state());
            return Optional.of(new CircuitBreakerSnapshot(state, record.failureCount(),
                    record.openedAt() == null ? null : Instant.parse(record.openedAt()),
                    record.retryAfter() == null ? null : Instant.parse(record.retryAfter())));
        } catch (Exception exception) {
            return Optional.empty();
        }
    }

    public boolean isRetryableFailure(String failureCode) {
        return "TIMEOUT".equals(failureCode) || "HTTP_429".equals(failureCode) || "HTTP_5XX".equals(failureCode)
                || "MODEL_NOT_FOUND".equals(failureCode) || "PROVIDER_EXCEPTION".equals(failureCode)
                || "PROVIDER_ERROR".equals(failureCode) || "INTERRUPTED".equals(failureCode);
    }

    public boolean isAuthFailure(String failureCode) {
        return "HTTP_401".equals(failureCode) || "HTTP_403".equals(failureCode) || "MODEL_NOT_FOUND".equals(failureCode)
                || "GEMINI_API_KEY_MISSING".equals(failureCode);
    }

    private boolean tryAcquireProbe(String circuitKey, CircuitRecord effective) {
        String probeKey = circuitKey + ":half-open-probe";
        boolean acquired = distributedCache.putIfAbsent(probeKey, Map.of("owner", "probe"), halfOpenLock());
        if (acquired) {
            return true;
        }
        // Redis unavailable: allow at most one local probe per open window.
        LocalCircuit local = localCircuits.get(circuitKey);
        if (local != null && local.state == CircuitState.OPEN) {
            local.state = CircuitState.HALF_OPEN;
            return true;
        }
        return false;
    }

    private Optional<CircuitRecord> readRemote(String circuitKey) {
        try {
            Optional<CircuitRecord> record = distributedCache.get(circuitKey, CircuitRecord.class);
            if (record.isPresent() && record.get().state() != null) {
                return record;
            }
            return Optional.empty();
        } catch (Exception exception) {
            return Optional.empty();
        }
    }

    private void writeRemote(String circuitKey, CircuitRecord record) {
        try {
            Duration retentionMargin = breakerProperties.getRetentionMargin() == null
                    ? Duration.ofMinutes(5)
                    : breakerProperties.getRetentionMargin();
            distributedCache.put(circuitKey, record, maxOpen().plus(retentionMargin));
        } catch (Exception ignored) {
            // local fallback already updated
        }
    }

    private int threshold() {
        return Math.max(1, breakerProperties.getFailureThreshold());
    }

    private Duration window() {
        return breakerProperties.getFailureWindow() == null ? Duration.ofSeconds(120)
                : breakerProperties.getFailureWindow();
    }

    private Duration halfOpenLock() {
        return breakerProperties.getHalfOpenLockTtl() == null ? Duration.ofSeconds(30)
                : breakerProperties.getHalfOpenLockTtl();
    }

    private Duration maxOpen() {
        return breakerProperties.getMaxOpenDuration() == null ? Duration.ofSeconds(300)
                : breakerProperties.getMaxOpenDuration();
    }

    Duration clampOpen(Duration retryAfter) {
        Duration min = breakerProperties.getMinOpenDuration() == null ? Duration.ofSeconds(30)
                : breakerProperties.getMinOpenDuration();
        Duration max = maxOpen();
        Duration base = breakerProperties.getOpenDuration() == null ? Duration.ofSeconds(60)
                : breakerProperties.getOpenDuration();
        Duration candidate = retryAfter == null ? base : retryAfter;
        if (candidate.compareTo(min) < 0) {
            return min;
        }
        if (candidate.compareTo(max) > 0) {
            return max;
        }
        return candidate;
    }

    private String providerOf(String circuitKey) {
        try {
            String[] parts = circuitKey.split(":");
            if (parts.length >= 6) {
                return parts[5];
            }
        } catch (Exception ignored) {
            // fall through
        }
        return "unknown";
    }

    private static Instant parseInstant(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Instant.parse(value);
        } catch (Exception exception) {
            return null;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CircuitRecord(String state, int failureCount, String openedAt, String retryAfter, String windowStart) {
    }

    private static final class LocalCircuit {
        private CircuitState state = CircuitState.CLOSED;
        private int failureCount;
        private Instant retryAfter;

        synchronized void onFailure() {
            failureCount++;
        }

        synchronized void open(Instant retryAt) {
            state = CircuitState.OPEN;
            retryAfter = retryAt;
        }

        synchronized void reset() {
            state = CircuitState.CLOSED;
            failureCount = 0;
            retryAfter = null;
        }

        synchronized CircuitRecord toRecord() {
            return new CircuitRecord(state.name(), failureCount,
                    null, retryAfter == null ? null : retryAfter.toString(), Instant.now().toString());
        }
    }
}
