package com.ban.vehicle_management.infrastructure.scheduler.parking;

import com.ban.vehicle_management.application.parking.checkinspacehold.port.in.CheckInSpaceHoldPortIn;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class CheckInSpaceHoldExpiryScheduler {
    private static final Logger LOGGER = LoggerFactory.getLogger(CheckInSpaceHoldExpiryScheduler.class);
    private final CheckInSpaceHoldPortIn holdPortIn;

    public CheckInSpaceHoldExpiryScheduler(CheckInSpaceHoldPortIn holdPortIn) {
        this.holdPortIn = holdPortIn;
    }

    @Scheduled(
            fixedDelayString = "${app.scheduler.check-in-space-hold-expiry.fixed-delay:20s}",
            initialDelayString = "${app.scheduler.check-in-space-hold-expiry.initial-delay:20s}"
    )
    public void expireDue() {
        try {
            int expired = holdPortIn.expireDue(Instant.now());
            if (expired > 0) {
                LOGGER.info("Expired {} check-in parking-space hold(s)", expired);
            }
        } catch (RuntimeException exception) {
            LOGGER.error("Check-in parking-space hold expiry failed", exception);
        }
    }
}
