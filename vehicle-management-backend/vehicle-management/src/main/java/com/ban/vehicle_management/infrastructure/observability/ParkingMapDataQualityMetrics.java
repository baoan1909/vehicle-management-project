package com.ban.vehicle_management.infrastructure.observability;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.concurrent.atomic.AtomicLong;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.parking.ParkingLotRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ParkingMapDataQualityMetrics {

    private final ParkingLotRepository parkingLotRepository;
    private final AtomicLong postGisAvailable = new AtomicLong(-1);
    private final AtomicLong activeWithoutLocation = new AtomicLong(-1);
    private final AtomicLong addressNeedsReview = new AtomicLong(-1);

    public ParkingMapDataQualityMetrics(ParkingLotRepository parkingLotRepository, MeterRegistry meterRegistry) {
        this.parkingLotRepository = parkingLotRepository;
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
            postGisAvailable.set(parkingLotRepository.findPostGisExtensionVersion() == null ? 0 : 1);
            activeWithoutLocation.set(parkingLotRepository.countActiveWithoutLocation());
            addressNeedsReview.set(parkingLotRepository.countAddressNeedsReview());
        } catch (RuntimeException exception) {
            postGisAvailable.set(0);
            activeWithoutLocation.set(-1);
            addressNeedsReview.set(-1);
        }
    }
}
