package com.ban.vehicle_management.infrastructure.observability;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.concurrent.atomic.AtomicLong;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.parking.ParkingSessionRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.parking.ParkingSpaceAllocationRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.parking.ParkingSpaceRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ParkingSpaceBusinessMetrics {

    private final ParkingSpaceRepository parkingSpaceRepository;
    private final ParkingSpaceAllocationRepository allocationRepository;
    private final ParkingSessionRepository parkingSessionRepository;
    private final AtomicLong activeSpaces = new AtomicLong(-1);
    private final AtomicLong occupiedSpaces = new AtomicLong(-1);
    private final AtomicLong activeAllocations = new AtomicLong(-1);
    private final AtomicLong openSessionsWithSpace = new AtomicLong(-1);

    public ParkingSpaceBusinessMetrics(
            ParkingSpaceRepository parkingSpaceRepository,
            ParkingSpaceAllocationRepository allocationRepository,
            ParkingSessionRepository parkingSessionRepository,
            MeterRegistry meterRegistry) {
        this.parkingSpaceRepository = parkingSpaceRepository;
        this.allocationRepository = allocationRepository;
        this.parkingSessionRepository = parkingSessionRepository;
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
            activeSpaces.set(parkingSpaceRepository.countActiveForMetrics());
            occupiedSpaces.set(parkingSpaceRepository.countOccupiedForMetrics());
            activeAllocations.set(allocationRepository.countActiveForMetrics());
            openSessionsWithSpace.set(parkingSessionRepository.countOpenWithSpaceForMetrics());
        } catch (RuntimeException exception) {
            activeSpaces.set(-1);
            occupiedSpaces.set(-1);
            activeAllocations.set(-1);
            openSessionsWithSpace.set(-1);
        }
    }

}
