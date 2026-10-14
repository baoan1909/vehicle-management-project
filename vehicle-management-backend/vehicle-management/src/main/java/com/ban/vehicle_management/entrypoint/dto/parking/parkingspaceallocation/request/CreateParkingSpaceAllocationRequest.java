package com.ban.vehicle_management.entrypoint.dto.parking.parkingspaceallocation.request;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record CreateParkingSpaceAllocationRequest(
        UUID subscriptionId,
        LocalDate effectiveFrom,
        LocalDate effectiveTo,
        Instant holdExpiresAt
) {
}
