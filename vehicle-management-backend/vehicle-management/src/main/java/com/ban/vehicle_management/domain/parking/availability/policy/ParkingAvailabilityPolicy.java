package com.ban.vehicle_management.domain.parking.availability.policy;

import com.ban.vehicle_management.domain.parking.availability.model.ParkingAvailabilitySnapshot;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import java.util.UUID;

/** Pure availability arithmetic shared by admission, summaries and realtime payloads. */
public class ParkingAvailabilityPolicy {

    public ParkingAvailabilitySnapshot calculateCapacity(UUID zoneId, long configuredCapacity,
            long unavailableCapacity, long subscriptionReserved, long occupiedRegistered,
            long occupiedRegisteredWithValidEntitlement, long occupiedVisitor) {
        requireNonNegative(configuredCapacity, "configuredCapacity");
        requireNonNegative(unavailableCapacity, "unavailableCapacity");
        if (unavailableCapacity > configuredCapacity) {
            throw new BadRequestException("unavailableCapacity must not exceed configuredCapacity");
        }
        long effective = configuredCapacity - unavailableCapacity;
        long physical = Math.max(0, effective - occupiedRegistered - occupiedVisitor);
        long reservedAbsent = Math.max(0,
                subscriptionReserved - occupiedRegisteredWithValidEntitlement);
        long visitor = Math.max(0, Math.min(physical, physical - reservedAbsent));
        return new ParkingAvailabilitySnapshot(zoneId, configuredCapacity, unavailableCapacity,
                effective, subscriptionReserved, occupiedRegistered, occupiedVisitor,
                physical, visitor, 0, 0, 0, 0);
    }

    public ParkingAvailabilitySnapshot calculateSpace(UUID zoneId, long configuredCapacity,
            long availableSpaces, long reservedSpaces, long occupiedRegistered,
            long occupiedVisitor, long maintenanceSpaces, long subscriptionReserved) {
        long occupied = occupiedRegistered + occupiedVisitor;
        long effective = Math.max(0, configuredCapacity - maintenanceSpaces);
        long physical = Math.max(0, effective - occupied);
        return new ParkingAvailabilitySnapshot(zoneId, configuredCapacity, maintenanceSpaces,
                effective, subscriptionReserved, occupiedRegistered, occupiedVisitor,
                physical, Math.max(0, availableSpaces), availableSpaces, reservedSpaces,
                occupied, maintenanceSpaces);
    }

    public void validateUnavailableCapacity(long configuredCapacity, long unavailableCapacity,
            long occupied, long committedReservations) {
        requireNonNegative(unavailableCapacity, "unavailableCapacity");
        if (unavailableCapacity > configuredCapacity) {
            throw new BadRequestException("unavailableCapacity must not exceed configuredCapacity");
        }
        long effective = configuredCapacity - unavailableCapacity;
        if (effective < occupied || effective < committedReservations) {
            throw new com.ban.vehicle_management.shared.exception.ConflictException(
                    "Unavailable capacity would break current occupancy or subscription commitments");
        }
    }

    private void requireNonNegative(long value, String field) {
        if (value < 0) {
            throw new BadRequestException(field + " must be non-negative");
        }
    }
}

