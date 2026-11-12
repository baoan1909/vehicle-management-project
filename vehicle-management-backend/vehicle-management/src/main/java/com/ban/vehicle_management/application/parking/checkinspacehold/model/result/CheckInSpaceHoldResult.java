package com.ban.vehicle_management.application.parking.checkinspacehold.model.result;

import com.ban.vehicle_management.shared.enumeration.parking.CheckInSpaceHoldStatus;
import java.time.Instant;
import java.util.UUID;

public record CheckInSpaceHoldResult(
        UUID holdId,
        UUID parkingLotId,
        UUID zoneId,
        UUID parkingSpaceId,
        String parkingSpaceCode,
        String customerType,
        CheckInSpaceHoldStatus status,
        Instant expiresAt,
        Instant serverTime,
        boolean created
) {
}
