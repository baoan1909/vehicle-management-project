package com.ban.vehicle_management.infrastructure.observability;

import com.ban.vehicle_management.application.parking.parkinglot.port.out.ParkingMapMetricsPortOut;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import org.springframework.stereotype.Component;

@Component
public class MicrometerParkingMapMetricsAdapter implements ParkingMapMetricsPortOut {

    private final MeterRegistry registry;
    private final Timer nearbyLatency;
    private final DistributionSummary nearbyResultCount;
    private final Map<String, Counter> geolocationCounters = new ConcurrentHashMap<>();

    public MicrometerParkingMapMetricsAdapter(MeterRegistry registry) {
        this.registry = registry;
        this.nearbyLatency = Timer.builder("maps.nearby.search.duration")
                .publishPercentiles(0.5, 0.95, 0.99)
                .sla(Duration.ofMillis(50), Duration.ofMillis(100), Duration.ofMillis(250), Duration.ofSeconds(1))
                .register(registry);
        this.nearbyResultCount = DistributionSummary.builder("maps.nearby.search.results")
                .publishPercentiles(0.5, 0.95, 0.99)
                .register(registry);
    }

    @Override
    public void recordNearbySearch(long durationNanos, int resultCount, boolean success) {
        nearbyLatency.record(durationNanos, TimeUnit.NANOSECONDS);
        nearbyResultCount.record(Math.max(resultCount, 0));
        registry.counter("maps.nearby.search.requests", "outcome", success ? "success" : "error").increment();
    }

    @Override
    public void recordGeolocationOutcome(String outcome) {
        geolocationCounters.computeIfAbsent(outcome, key -> Counter.builder("maps.geolocation.outcomes")
                .tag("outcome", key)
                .register(registry)).increment();
    }
}
