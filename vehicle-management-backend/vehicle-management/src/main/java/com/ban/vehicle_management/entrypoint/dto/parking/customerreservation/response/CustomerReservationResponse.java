package com.ban.vehicle_management.entrypoint.dto.parking.customerreservation.response;

import com.ban.vehicle_management.shared.enumeration.parking.CustomerReservationStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record CustomerReservationResponse(UUID reservationId, UUID parkingLotId, UUID zoneId,
        UUID parkingSpaceId, UUID vehicleTypeId, String licensePlate,
        CustomerReservationStatus status, UUID invoiceId, BigDecimal prepaidAmount,
        Instant paymentHoldExpiresAt, Instant paidAt, Instant checkInDeadline,
        String qrPayload, String qrCodeDataUrl, String accessToken,
        String parkingLevelName, String zoneName, String parkingSpaceCode,
        String parkingLotName) { }
