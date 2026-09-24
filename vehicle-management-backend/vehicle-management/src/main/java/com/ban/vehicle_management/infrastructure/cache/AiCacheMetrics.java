package com.ban.vehicle_management.infrastructure.cache;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/**
 * Minimal cache/circuit metrics. High-cardinality values (model id, tenant id)
 * are never used as tags; only bounded cache namespaces and reasons are.
 */
@Component
public class AiCacheMetrics {

    private final MeterRegistry registry;
    private final Map<String, Counter> counters = new ConcurrentHashMap<>();
    private final Map<String, Timer> timers = new ConcurrentHashMap<>();

    public AiCacheMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    public void hit(String cache) {
        counter("cache_hit_total", "cache", cache).increment();
    }

    public void miss(String cache) {
        counter("cache_miss_total", "cache", cache).increment();
    }

    public void put(String cache) {
        counter("cache_put_total", "cache", cache).increment();
    }

    public void error(String cache, String operation) {
        counter("cache_error_total", "cache", cache, "operation", operation).increment();
    }

    public void invalidPayload(String cache) {
        counter("cache_invalid_payload_total", "cache", cache).increment();
    }

    public void circuitOpen(String provider, String useCase) {
        counter("circuit_open_total", "provider", provider, "usecase", useCase).increment();
    }

    public void circuitHalfOpen(String provider, String useCase) {
        counter("circuit_half_open_total", "provider", provider, "usecase", useCase).increment();
    }

    public void circuitClose(String provider, String useCase) {
        counter("circuit_close_total", "provider", provider, "usecase", useCase).increment();
    }

    public void fallbackAttempt() {
        counter("ai_fallback_attempt_total").increment();
    }

    public void candidateSkipped(String reason) {
        counter("ai_candidate_skipped_total", "reason", reason).increment();
    }

    public void singleFlightContention() {
        counter("single_flight_lock_contention_total").increment();
    }

    public Timer.Sample latencySample() {
        return Timer.start(registry);
    }

    public void observeLatency(Timer.Sample sample, String cache) {
        try {
            sample.stop(timer("cache_operation_latency", "cache", cache));
        } catch (Exception ignored) {
            // metrics must never break business flow
        }
    }

    private Counter counter(String name, String... tags) {
        String key = name + java.util.Arrays.toString(tags);
        return counters.computeIfAbsent(key, ignored -> Counter.builder(name).tags(tags).register(registry));
    }

    private Timer timer(String name, String... tags) {
        String key = name + java.util.Arrays.toString(tags);
        return timers.computeIfAbsent(key, ignored -> Timer.builder(name).tags(tags).register(registry));
    }
}
