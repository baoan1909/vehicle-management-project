package com.ban.vehicle_management.entrypoint.dto.parking.customerreservation.response;

import java.time.Instant;

public record ReservationCheckoutResponse(
        CustomerReservationResponse reservation,
        String paymentUrl,
        String paymentMethod,
        String transactionRef,
        String bankCode,
        String accountNumber,
        String accountHolder,
        Instant paymentExpiresAt
) { }
