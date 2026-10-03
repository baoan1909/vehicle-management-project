package com.ban.vehicle_management.infrastructure.location;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Component;

@Component
public class MapProviderMetrics {

    private final MeterRegistry registry;
    private final Map<String, Counter> counters = new ConcurrentHashMap<>();
    private final Map<String, Timer> timers = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> quotaRemaining = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> quotaLimit = new ConcurrentHashMap<>();

    public MapProviderMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    public void cacheRequest(String provider, MapProviderOperation operation, boolean hit) {
        counter(
                "maps.geocoding.cache.requests",
                "provider", provider,
                "operation", operation.name().toLowerCase(),
                "result", hit ? "hit" : "miss"
        ).increment();
    }

    public Timer.Sample providerRequestStarted() {
        return Timer.start(registry);
    }

    public void providerRequestFinished(
            Timer.Sample sample,
            String provider,
            MapProviderOperation operation,
            String outcome
    ) {
        sample.stop(timer(
                "maps.provider.request.duration",
                "provider", provider,
                "operation", operation.name().toLowerCase(),
                "outcome", outcome
        ));
    }

    public void providerError(String provider, MapProviderOperation operation, String reason) {
        counter(
                "maps.provider.errors",
                "provider", provider,
                "operation", operation.name().toLowerCase(),
                "reason", reason
        ).increment();
    }

    public void quotaExhausted(String provider, MapProviderOperation operation) {
        counter(
                "maps.provider.quota.exhausted",
                "provider", provider,
                "operation", operation.name().toLowerCase()
        ).increment();
    }

    public void updateQuota(String provider, MapProviderOperation operation, long remaining, long limit) {
        String key = provider + ':' + operation.name().toLowerCase();
        quotaRemaining.computeIfAbsent(key, ignored -> registerGauge(
                "maps.provider.quota.remaining", provider, operation)).set(Math.max(remaining, 0));
        quotaLimit.computeIfAbsent(key, ignored -> registerGauge(
                "maps.provider.quota.limit", provider, operation)).set(limit);
    }

    private AtomicLong registerGauge(String name, String provider, MapProviderOperation operation) {
        AtomicLong value = new AtomicLong();
        Gauge.builder(name, value, AtomicLong::get)
                .tag("provider", provider)
                .tag("operation", operation.name().toLowerCase())
                .register(registry);
        return value;
    }

    private Counter counter(String name, String... tags) {
        String key = name + '|' + String.join("|", tags);
        return counters.computeIfAbsent(key, ignored -> Counter.builder(name).tags(tags).register(registry));
    }

    private Timer timer(String name, String... tags) {
        String key = name + '|' + String.join("|", tags);
        return timers.computeIfAbsent(key, ignored -> Timer.builder(name)
                .publishPercentiles(0.5, 0.95, 0.99)
                .tags(tags)
                .register(registry));
    }
}
