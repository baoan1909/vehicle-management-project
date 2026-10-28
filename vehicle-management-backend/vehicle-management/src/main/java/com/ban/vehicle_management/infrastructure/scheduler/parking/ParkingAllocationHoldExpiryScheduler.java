package com.ban.vehicle_management.infrastructure.scheduler.parking;

import com.ban.vehicle_management.application.parking.parkingspaceallocation.port.in.ParkingSpaceAllocationPortIn;
import com.ban.vehicle_management.application.parking.allocation.SubscriptionParkingAllocationService;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ParkingAllocationHoldExpiryScheduler {

    private static final Logger LOGGER = LoggerFactory.getLogger(ParkingAllocationHoldExpiryScheduler.class);

    private final ParkingSpaceAllocationPortIn allocationPortIn;
    private final SubscriptionParkingAllocationService subscriptionAllocationService;

    public ParkingAllocationHoldExpiryScheduler(ParkingSpaceAllocationPortIn allocationPortIn,
            SubscriptionParkingAllocationService subscriptionAllocationService) {
        this.allocationPortIn = allocationPortIn;
        this.subscriptionAllocationService = subscriptionAllocationService;
    }

    @Scheduled(
            fixedDelayString = "${app.scheduler.parking-allocation-hold-expiry.fixed-delay:5m}",
            initialDelayString = "${app.scheduler.parking-allocation-hold-expiry.initial-delay:1m}"
    )
    public void expireHeldAllocations() {
        try {
            int expired = allocationPortIn.expireHeldAllocations();
            int expiredCapacity = subscriptionAllocationService.expireCapacityHolds(Instant.now());
            if (expired + expiredCapacity > 0) {
                LOGGER.info("Parking allocation hold expiry processed {} space and {} capacity hold(s)",
                        expired, expiredCapacity);
            }
        } catch (RuntimeException exception) {
            LOGGER.error("Parking allocation hold expiry failed", exception);
        }
    }
}
