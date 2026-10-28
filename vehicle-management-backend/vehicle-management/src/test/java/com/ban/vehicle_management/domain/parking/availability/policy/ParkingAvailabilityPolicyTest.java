package com.ban.vehicle_management.domain.parking.availability.policy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.ban.vehicle_management.shared.exception.BadRequestException;
import com.ban.vehicle_management.shared.exception.ConflictException;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ParkingAvailabilityPolicyTest {

    private final ParkingAvailabilityPolicy policy = new ParkingAvailabilityPolicy();

    @Test
    void visitorIsRejectedWhenSubscriptionsConsumeAdmissionCapacity() {
        var result = policy.calculateCapacity(UUID.randomUUID(), 5, 0,
                4, 0, 0, 1);

        assertEquals(4, result.physicalAvailable());
        assertEquals(0, result.visitorAvailable());
    }

    @Test
    void registeredCheckInConsumesPhysicalButNotVisitorAvailability() {
        UUID zoneId = UUID.randomUUID();
        var before = policy.calculateCapacity(zoneId, 10, 1,
                4, 0, 0, 2);
        var after = policy.calculateCapacity(zoneId, 10, 1,
                4, 1, 1, 2);

        assertEquals(before.physicalAvailable() - 1, after.physicalAvailable());
        assertEquals(before.visitorAvailable(), after.visitorAvailable());
    }

    @Test
    void registeredCheckoutRestoresPhysicalButKeepsReservationUnavailableToVisitors() {
        UUID zoneId = UUID.randomUUID();
        var before = policy.calculateCapacity(zoneId, 10, 1,
                4, 1, 1, 2);
        var after = policy.calculateCapacity(zoneId, 10, 1,
                4, 0, 0, 2);

        assertEquals(before.physicalAvailable() + 1, after.physicalAvailable());
        assertEquals(before.visitorAvailable(), after.visitorAvailable());
    }

    @Test
    void expiredEntitlementNeverMakesVisitorAvailabilityExceedPhysicalAvailability() {
        var result = policy.calculateCapacity(UUID.randomUUID(), 10, 0,
                0, 1, 0, 7);

        assertEquals(2, result.physicalAvailable());
        assertEquals(2, result.visitorAvailable());
    }

    @Test
    void unavailableCapacityMustRespectConfigurationOccupancyAndCommitments() {
        assertThrows(BadRequestException.class,
                () -> policy.validateUnavailableCapacity(5, 6, 0, 0));
        assertThrows(ConflictException.class,
                () -> policy.validateUnavailableCapacity(10, 4, 7, 5));
        assertThrows(ConflictException.class,
                () -> policy.validateUnavailableCapacity(10, 4, 5, 7));
    }
}
