package com.ban.vehicle_management.infrastructure.scheduler.parking;

import com.ban.vehicle_management.application.parking.parkingspaceallocation.port.in.ParkingSpaceAllocationPortIn;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ParkingAllocationHoldExpiryScheduler {

    private static final Logger LOGGER = LoggerFactory.getLogger(ParkingAllocationHoldExpiryScheduler.class);

    private final ParkingSpaceAllocationPortIn allocationPortIn;

    public ParkingAllocationHoldExpiryScheduler(ParkingSpaceAllocationPortIn allocationPortIn) {
        this.allocationPortIn = allocationPortIn;
    }

    @Scheduled(
            fixedDelayString = "${app.scheduler.parking-allocation-hold-expiry.fixed-delay:5m}",
            initialDelayString = "${app.scheduler.parking-allocation-hold-expiry.initial-delay:1m}"
    )
    public void expireHeldAllocations() {
        try {
            int expired = allocationPortIn.expireHeldAllocations();
            if (expired > 0) {
                LOGGER.info("Parking allocation hold expiry processed {} expired hold(s)", expired);
            }
        } catch (RuntimeException exception) {
            LOGGER.error("Parking allocation hold expiry failed", exception);
        }
    }
}
