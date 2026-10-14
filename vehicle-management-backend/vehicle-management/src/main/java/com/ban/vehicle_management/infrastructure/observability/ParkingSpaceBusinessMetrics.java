package com.ban.vehicle_management.infrastructure.observability;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ParkingSpaceBusinessMetrics {

    private final JdbcTemplate jdbcTemplate;
    private final AtomicLong activeSpaces = new AtomicLong(-1);
    private final AtomicLong occupiedSpaces = new AtomicLong(-1);
    private final AtomicLong activeAllocations = new AtomicLong(-1);
    private final AtomicLong openSessionsWithSpace = new AtomicLong(-1);

    public ParkingSpaceBusinessMetrics(JdbcTemplate jdbcTemplate, MeterRegistry meterRegistry) {
        this.jdbcTemplate = jdbcTemplate;
        Gauge.builder("parking.spaces.active", activeSpaces, AtomicLong::get).register(meterRegistry);
        Gauge.builder("parking.spaces.occupied", occupiedSpaces, AtomicLong::get).register(meterRegistry);
        Gauge.builder("parking.allocations.active", activeAllocations, AtomicLong::get).register(meterRegistry);
        Gauge.builder("parking.sessions.open.with-space", openSessionsWithSpace, AtomicLong::get)
                .register(meterRegistry);
    }

    @Scheduled(
            fixedDelayString = "${app.parking.metrics.business-refresh-ms:300000}",
            initialDelayString = "${app.parking.metrics.business-initial-delay-ms:20000}"
    )
    public void refresh() {
        try {
            activeSpaces.set(count(
                    "SELECT COUNT(*) FROM parking.parking_spaces WHERE lifecycle_status = 'ACTIVE'"));
            occupiedSpaces.set(count(
                    "SELECT COUNT(*) FROM parking.parking_spaces WHERE lifecycle_status = 'ACTIVE' AND status = 'OCCUPIED'"));
            activeAllocations.set(count(
                    "SELECT COUNT(*) FROM parking.parking_space_allocations WHERE allocation_status IN ('HELD', 'ACTIVE')"));
            openSessionsWithSpace.set(count(
                    "SELECT COUNT(*) FROM parking.parking_sessions WHERE status = 'OPEN' AND parking_space_id IS NOT NULL"));
        } catch (RuntimeException exception) {
            activeSpaces.set(-1);
            occupiedSpaces.set(-1);
            activeAllocations.set(-1);
            openSessionsWithSpace.set(-1);
        }
    }

    private long count(String sql) {
        Long value = jdbcTemplate.queryForObject(sql, Long.class);
        return value == null ? -1 : value;
    }
}
