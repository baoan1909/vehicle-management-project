package com.ban.vehicle_management.infrastructure.observability;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ParkingMapDataQualityMetrics {

    private final JdbcTemplate jdbcTemplate;
    private final AtomicLong postGisAvailable = new AtomicLong(-1);
    private final AtomicLong activeWithoutLocation = new AtomicLong(-1);
    private final AtomicLong addressNeedsReview = new AtomicLong(-1);

    public ParkingMapDataQualityMetrics(JdbcTemplate jdbcTemplate, MeterRegistry meterRegistry) {
        this.jdbcTemplate = jdbcTemplate;
        Gauge.builder("maps.postgis.available", postGisAvailable, AtomicLong::get).register(meterRegistry);
        Gauge.builder("maps.parking.active.without.location", activeWithoutLocation, AtomicLong::get)
                .register(meterRegistry);
        Gauge.builder("maps.parking.address.needs.review", addressNeedsReview, AtomicLong::get)
                .register(meterRegistry);
    }

    @Scheduled(
            fixedDelayString = "${app.maps.metrics.data-quality-refresh-ms:300000}",
            initialDelayString = "${app.maps.metrics.data-quality-initial-delay-ms:15000}"
    )
    public void refresh() {
        try {
            Long extensionCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM pg_extension WHERE extname = 'postgis'",
                    Long.class
            );
            Long missingLocationCount = jdbcTemplate.queryForObject("""
                    SELECT COUNT(*)
                    FROM parking.parking_lots
                    WHERE status = 'ACTIVE' AND location IS NULL
                    """, Long.class);
            Long needsReviewCount = jdbcTemplate.queryForObject("""
                    SELECT COUNT(*)
                    FROM parking.parking_lots
                    WHERE geocoding_status = 'NEEDS_REVIEW'
                    """, Long.class);
            postGisAvailable.set(extensionCount != null && extensionCount > 0 ? 1 : 0);
            activeWithoutLocation.set(missingLocationCount == null ? -1 : missingLocationCount);
            addressNeedsReview.set(needsReviewCount == null ? -1 : needsReviewCount);
        } catch (RuntimeException exception) {
            postGisAvailable.set(0);
            activeWithoutLocation.set(-1);
            addressNeedsReview.set(-1);
        }
    }
}
