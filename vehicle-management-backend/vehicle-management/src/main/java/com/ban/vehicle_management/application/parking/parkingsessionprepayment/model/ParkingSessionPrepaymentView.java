package com.ban.vehicle_management.application.parking.parkingsessionprepayment.model;

import com.ban.vehicle_management.shared.enumeration.parking.ParkingSessionPrepaymentStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ParkingSessionPrepaymentView(
        UUID prepaymentId,
        UUID invoiceId,
        UUID parkingSessionId,
        String licensePlate,
        String cardNumber,
        String parkingLotName,
        Instant checkInTime,
        Instant estimatedCheckOutAt,
        BigDecimal projectedGrossAmount,
        BigDecimal paidBeforeAmount,
        BigDecimal amount,
        ParkingSessionPrepaymentStatus status,
        Instant expiresAt,
        Instant paidAt
) {
}
