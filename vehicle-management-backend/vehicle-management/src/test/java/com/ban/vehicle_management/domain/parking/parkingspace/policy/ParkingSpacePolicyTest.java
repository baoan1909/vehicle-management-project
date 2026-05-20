package com.ban.vehicle_management.domain.parking.parkingspace.policy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.ban.vehicle_management.domain.parking.parkingspace.model.ParkingSpace;
import com.ban.vehicle_management.shared.enumeration.parking.ParkingSpaceStatus;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ParkingSpacePolicyTest {

    private final ParkingSpacePolicy parkingSpacePolicy = new ParkingSpacePolicy();

    @Test
    void shouldInitializeParkingSpaceWithAvailableStatus() {
        ParkingSpace parkingSpace = new ParkingSpace();
        parkingSpace.setZoneId(UUID.randomUUID());
        parkingSpace.setCode(" P-01 ");

        parkingSpacePolicy.initialize(parkingSpace);

        assertEquals("P-01", parkingSpace.getCode());
        assertEquals(ParkingSpaceStatus.AVAILABLE, parkingSpace.getStatus());
    }

    @Test
    void shouldOccupyAvailableParkingSpace() {
        ParkingSpace parkingSpace = validParkingSpace(ParkingSpaceStatus.AVAILABLE);

        parkingSpacePolicy.occupy(parkingSpace);

        assertEquals(ParkingSpaceStatus.OCCUPIED, parkingSpace.getStatus());
    }

    @Test
    void shouldRejectMaintenanceForOccupiedParkingSpace() {
        ParkingSpace parkingSpace = validParkingSpace(ParkingSpaceStatus.OCCUPIED);

        assertThrows(BadRequestException.class, () -> parkingSpacePolicy.markMaintenance(parkingSpace));
    }

    @Test
    void shouldRejectParkingSpaceCodeWithUnsupportedCharacters() {
        ParkingSpace parkingSpace = new ParkingSpace();
        parkingSpace.setZoneId(UUID.randomUUID());
        parkingSpace.setCode("P<01>");

        assertThrows(BadRequestException.class, () -> parkingSpacePolicy.initialize(parkingSpace));
    }

    private ParkingSpace validParkingSpace(ParkingSpaceStatus status) {
        ParkingSpace parkingSpace = new ParkingSpace();
        parkingSpace.setZoneId(UUID.randomUUID());
        parkingSpace.setCode("P-01");
        parkingSpace.setStatus(status);
        return parkingSpace;
    }
}

