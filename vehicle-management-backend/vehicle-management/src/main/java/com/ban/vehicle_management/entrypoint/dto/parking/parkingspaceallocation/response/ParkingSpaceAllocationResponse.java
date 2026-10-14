package com.ban.vehicle_management.entrypoint.dto.parking.parkingspaceallocation.response;

import com.ban.vehicle_management.shared.enumeration.parking.ParkingSpaceAllocationStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record ParkingSpaceAllocationResponse(
        UUID allocationId,
        UUID parkingSpaceId,
        UUID subscriptionId,
        LocalDate effectiveFrom,
        LocalDate effectiveTo,
        Instant holdExpiresAt,
        ParkingSpaceAllocationStatus allocationStatus,
        Instant createdAt,
        UUID createdBy,
        Instant updatedAt,
        UUID updatedBy
) {
}
