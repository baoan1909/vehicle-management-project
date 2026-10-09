package com.ban.vehicle_management.domain.parking.parkingspaceallocation.policy;

import com.ban.vehicle_management.domain.parking.parkingspaceallocation.model.ParkingSpaceAllocation;
import com.ban.vehicle_management.shared.enumeration.parking.ParkingSpaceAllocationStatus;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import java.time.Instant;
import java.time.LocalDate;

public class ParkingSpaceAllocationPolicy {

    public void initialize(ParkingSpaceAllocation allocation) {
        requireAllocation(allocation);
        if (allocation.getEffectiveFrom() == null) {
            throw new BadRequestException("effectiveFrom must not be null");
        }
        if (allocation.getEffectiveTo() == null) {
            throw new BadRequestException("effectiveTo must not be null");
        }
        if (allocation.getAllocationStatus() == null) {
            allocation.setAllocationStatus(ParkingSpaceAllocationStatus.HELD);
        }
        validateState(allocation);
    }

    public void activate(ParkingSpaceAllocation allocation) {
        requireAllocation(allocation);
        if (allocation.getAllocationStatus() != ParkingSpaceAllocationStatus.HELD) {
            throw new BadRequestException("Only HELD allocation can be activated");
        }
        allocation.setAllocationStatus(ParkingSpaceAllocationStatus.ACTIVE);
        allocation.setHoldExpiresAt(null);
        validateState(allocation);
    }

    public void release(ParkingSpaceAllocation allocation) {
        requireAllocation(allocation);
        if (allocation.getAllocationStatus() == ParkingSpaceAllocationStatus.CANCELLED) {
            throw new BadRequestException("Cannot release cancelled allocation");
        }
        allocation.setAllocationStatus(ParkingSpaceAllocationStatus.RELEASED);
        validateState(allocation);
    }

    public void cancel(ParkingSpaceAllocation allocation) {
        requireAllocation(allocation);
        allocation.setAllocationStatus(ParkingSpaceAllocationStatus.CANCELLED);
        validateState(allocation);
    }

    public void setHoldExpiration(ParkingSpaceAllocation allocation, Instant holdExpiresAt) {
        requireAllocation(allocation);
        if (allocation.getAllocationStatus() != ParkingSpaceAllocationStatus.HELD) {
            throw new BadRequestException("Hold expiration can only be set on HELD allocation");
        }
        allocation.setHoldExpiresAt(holdExpiresAt);
        validateState(allocation);
    }

    public void validateState(ParkingSpaceAllocation allocation) {
        requireAllocation(allocation);
        requireField(allocation.getParkingSpaceId(), "parkingSpaceId");
        requireField(allocation.getSubscriptionId(), "subscriptionId");
        requireField(allocation.getEffectiveFrom(), "effectiveFrom");
        requireField(allocation.getEffectiveTo(), "effectiveTo");
        requireField(allocation.getAllocationStatus(), "allocationStatus");

        if (allocation.getEffectiveTo().isBefore(allocation.getEffectiveFrom())) {
            throw new BadRequestException("effectiveTo must not be before effectiveFrom");
        }
    }

    /**
     * Application-level overlap guard for friendly errors. The database exclusion
     * constraint remains the authoritative backstop against concurrent races.
     */
    public void validateNoOverlap(ParkingSpaceAllocation allocation,
            java.util.List<ParkingSpaceAllocation> existingActive) {
        requireAllocation(allocation);
        if (existingActive == null) {
            return;
        }
        for (ParkingSpaceAllocation existing : existingActive) {
            if (existing == null || existing.getAllocationId() != null
                    && existing.getAllocationId().equals(allocation.getAllocationId())) {
                continue;
            }
            if (existing.getAllocationStatus() != ParkingSpaceAllocationStatus.HELD
                    && existing.getAllocationStatus() != ParkingSpaceAllocationStatus.ACTIVE) {
                continue;
            }
            if (!existing.getParkingSpaceId().equals(allocation.getParkingSpaceId())) {
                continue;
            }
            boolean intersects = !existing.getEffectiveFrom().isAfter(allocation.getEffectiveTo())
                    && !existing.getEffectiveTo().isBefore(allocation.getEffectiveFrom());
            if (intersects) {
                throw new BadRequestException("Parking space already allocated for an overlapping period");
            }
        }
    }

    public boolean isActiveOn(ParkingSpaceAllocation allocation, LocalDate date) {
        return allocation.getAllocationStatus() == ParkingSpaceAllocationStatus.ACTIVE
                && !allocation.getEffectiveFrom().isAfter(date)
                && !allocation.getEffectiveTo().isBefore(date);
    }

    public boolean isHoldExpired(ParkingSpaceAllocation allocation, Instant now) {
        return allocation.getAllocationStatus() == ParkingSpaceAllocationStatus.HELD
                && allocation.getHoldExpiresAt() != null
                && !allocation.getHoldExpiresAt().isAfter(now);
    }

    private void requireAllocation(ParkingSpaceAllocation allocation) {
        requireField(allocation, "allocation");
    }

    private void requireField(Object value, String fieldName) {
        if (value == null) {
            throw new BadRequestException(fieldName + " must not be null");
        }
    }
}