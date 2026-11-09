package com.ban.vehicle_management.infrastructure.scheduler.parking;

import com.ban.vehicle_management.application.parking.customerreservation.port.in.CustomerReservationExpiryPortIn;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class CustomerReservationExpiryScheduler {
    private static final Logger LOGGER = LoggerFactory.getLogger(CustomerReservationExpiryScheduler.class);
    private final CustomerReservationExpiryPortIn reservations;

    public CustomerReservationExpiryScheduler(CustomerReservationExpiryPortIn reservations) {
        this.reservations = reservations;
    }

    @Scheduled(fixedDelayString = "${app.scheduler.customer-reservation-expiry.fixed-delay:1m}")
    public void expireDue() {
        try {
            int expired = reservations.expireDue();
            if (expired > 0) {
                LOGGER.info("Expired {} customer reservation holds", expired);
            }
        } catch (RuntimeException exception) {
            LOGGER.error("Customer reservation expiry failed", exception);
        }
    }
}
