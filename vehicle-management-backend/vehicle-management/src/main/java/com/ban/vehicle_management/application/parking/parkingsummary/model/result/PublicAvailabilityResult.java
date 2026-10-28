package com.ban.vehicle_management.application.parking.parkingsummary.model.result;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record PublicAvailabilityResult(
        UUID parkingLotId,
        String parkingLotName,
        String parkingLotStatus,
        long configuredCapacity,
        long unavailableCapacity,
        long effectiveCapacity,
        long subscriptionReserved,
        long occupiedRegistered,
        long occupiedVisitor,
        long physicalAvailable,
        long total,
        long visitorAvailable,
        long subscriptionAvailable,
        boolean hasAvailability,
        Instant snapshotAt,
        List<VehicleTypeAvailability> breakdown
) {
    public record VehicleTypeAvailability(
            UUID vehicleTypeId,
            long total,
            long visitorAvailable,
            long subscriptionAvailable
    ) {
    }
}
