package com.ban.vehicle_management.application.parking.customerreservation.model;

import java.time.Instant;

public record ReservationCheckout(
        ReservationView reservation,
        String paymentUrl,
        String paymentMethod,
        String transactionRef,
        String bankCode,
        String accountNumber,
        String accountHolder,
        Instant paymentExpiresAt
) { }
