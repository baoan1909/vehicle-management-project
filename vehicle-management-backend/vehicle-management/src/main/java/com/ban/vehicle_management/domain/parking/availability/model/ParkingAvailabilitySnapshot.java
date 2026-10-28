package com.ban.vehicle_management.domain.parking.availability.model;

import java.util.UUID;

public record ParkingAvailabilitySnapshot(
        UUID zoneId,
        long configuredCapacity,
        long unavailableCapacity,
        long effectiveCapacity,
        long subscriptionReserved,
        long occupiedRegistered,
        long occupiedVisitor,
        long physicalAvailable,
        long visitorAvailable,
        long availableSpaceCount,
        long reservedSpaceCount,
        long occupiedSpaceCount,
        long maintenanceSpaceCount
) {
}

