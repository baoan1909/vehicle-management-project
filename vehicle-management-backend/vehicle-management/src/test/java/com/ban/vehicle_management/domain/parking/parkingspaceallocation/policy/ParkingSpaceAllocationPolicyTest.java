package com.ban.vehicle_management.domain.parking.parkingspaceallocation.policy;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.ban.vehicle_management.domain.parking.parkingspaceallocation.model.ParkingSpaceAllocation;
import com.ban.vehicle_management.shared.enumeration.parking.ParkingSpaceAllocationStatus;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ParkingSpaceAllocationPolicyTest {

    private final ParkingSpaceAllocationPolicy policy = new ParkingSpaceAllocationPolicy();

    @Test
    void shouldRejectPartiallyOverlappingPeriod() {
        UUID spaceId = UUID.randomUUID();
        ParkingSpaceAllocation requested = allocation(spaceId,
                LocalDate.of(2026, 10, 15), LocalDate.of(2026, 10, 20));
        ParkingSpaceAllocation existing = allocation(spaceId,
                LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31));

        assertThrows(BadRequestException.class,
                () -> policy.validateNoOverlap(requested, List.of(existing)));
    }

    @Test
    void shouldAcceptAdjacentPeriod() {
        UUID spaceId = UUID.randomUUID();
        ParkingSpaceAllocation requested = allocation(spaceId,
                LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30));
        ParkingSpaceAllocation existing = allocation(spaceId,
                LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31));

        assertDoesNotThrow(() -> policy.validateNoOverlap(requested, List.of(existing)));
    }

    @Test
    void shouldIgnoreReleasedAllocationsAndSelf() {
        UUID spaceId = UUID.randomUUID();
        ParkingSpaceAllocation requested = allocation(spaceId,
                LocalDate.of(2026, 10, 15), LocalDate.of(2026, 10, 20));
        ParkingSpaceAllocation released = allocation(spaceId,
                LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31));
        released.setAllocationStatus(ParkingSpaceAllocationStatus.RELEASED);
        ParkingSpaceAllocation self = allocation(spaceId,
                LocalDate.of(2026, 10, 15), LocalDate.of(2026, 10, 20));
        self.setAllocationId(requested.getAllocationId());

        assertDoesNotThrow(() -> policy.validateNoOverlap(requested, List.of(released, self)));
    }

    @Test
    void shouldExpireHoldAtItsExactDeadline() {
        ParkingSpaceAllocation held = allocation(UUID.randomUUID(),
                LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31));
        held.setAllocationStatus(ParkingSpaceAllocationStatus.HELD);
        java.time.Instant deadline = java.time.Instant.parse("2026-10-09T01:00:00Z");
        held.setHoldExpiresAt(deadline);

        assertTrue(policy.isHoldExpired(held, deadline));
    }

    private ParkingSpaceAllocation allocation(UUID spaceId, LocalDate from, LocalDate to) {
        ParkingSpaceAllocation allocation = new ParkingSpaceAllocation();
        allocation.setAllocationId(UUID.randomUUID());
        allocation.setParkingSpaceId(spaceId);
        allocation.setSubscriptionId(UUID.randomUUID());
        allocation.setEffectiveFrom(from);
        allocation.setEffectiveTo(to);
        allocation.setAllocationStatus(ParkingSpaceAllocationStatus.ACTIVE);
        return allocation;
    }
}
