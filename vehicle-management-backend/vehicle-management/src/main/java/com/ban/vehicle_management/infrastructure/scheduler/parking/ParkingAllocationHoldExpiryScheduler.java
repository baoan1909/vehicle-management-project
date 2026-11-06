package com.ban.vehicle_management.infrastructure.scheduler.parking;

import com.ban.vehicle_management.application.parking.parkingspaceallocation.port.in.ParkingSpaceAllocationPortIn;
import com.ban.vehicle_management.application.parking.allocation.SubscriptionParkingAllocationService;
import com.ban.vehicle_management.application.accesscontrol.subscription.port.in.SubscriptionPortIn;
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
    private final SubscriptionPortIn subscriptionPortIn;

    public ParkingAllocationHoldExpiryScheduler(ParkingSpaceAllocationPortIn allocationPortIn,
            SubscriptionParkingAllocationService subscriptionAllocationService,
            SubscriptionPortIn subscriptionPortIn) {
        this.allocationPortIn = allocationPortIn;
        this.subscriptionAllocationService = subscriptionAllocationService;
        this.subscriptionPortIn = subscriptionPortIn;
    }

    @Scheduled(
            fixedDelayString = "${app.scheduler.parking-allocation-hold-expiry.fixed-delay:5m}",
            initialDelayString = "${app.scheduler.parking-allocation-hold-expiry.initial-delay:1m}"
    )
    public void expireHeldAllocations() {
        try {
            int autoFinalized = subscriptionPortIn.finalizeExpiredSpaceSelections(Instant.now());
            int expired = allocationPortIn.expireHeldAllocations();
            int expiredCapacity = subscriptionAllocationService.expireCapacityHolds(Instant.now());
            if (autoFinalized + expired + expiredCapacity > 0) {
                LOGGER.info("Parking allocation scheduler finalized {} selection(s), expired {} space and {} capacity hold(s)",
                        autoFinalized, expired, expiredCapacity);
            }
        } catch (RuntimeException exception) {
            LOGGER.error("Parking allocation hold expiry failed", exception);
        }
    }
}
