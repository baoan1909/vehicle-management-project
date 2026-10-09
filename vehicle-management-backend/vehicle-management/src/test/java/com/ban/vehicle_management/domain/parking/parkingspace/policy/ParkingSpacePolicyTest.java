package com.ban.vehicle_management.domain.parking.parkingspace.policy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.ban.vehicle_management.domain.parking.parkingspace.model.ParkingSpace;
import com.ban.vehicle_management.shared.enumeration.parking.ManualOccupancyType;
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

        parkingSpacePolicy.occupy(parkingSpace, ManualOccupancyType.VISITOR, "Manual parking");

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

    @Test
    void shouldRequireReasonWhenOccupyingManually() {
        ParkingSpace parkingSpace = validParkingSpace(ParkingSpaceStatus.AVAILABLE);
        parkingSpace.setStatusReason(null);

        assertThrows(BadRequestException.class,
                () -> parkingSpacePolicy.occupy(parkingSpace, ManualOccupancyType.VISITOR, null));
    }

    @Test
    void shouldRejectPartialOrNegativeOperationalGeometry() {
        ParkingSpace partial = validParkingSpace(ParkingSpaceStatus.AVAILABLE);
        partial.setX(java.math.BigDecimal.ONE);
        assertThrows(BadRequestException.class, () -> parkingSpacePolicy.validateState(partial));

        ParkingSpace negative = validParkingSpace(ParkingSpaceStatus.AVAILABLE);
        negative.setX(java.math.BigDecimal.valueOf(-1));
        negative.setY(java.math.BigDecimal.ZERO);
        negative.setWidth(java.math.BigDecimal.ONE);
        negative.setHeight(java.math.BigDecimal.ONE);
        assertThrows(BadRequestException.class, () -> parkingSpacePolicy.validateState(negative));
    }

    private ParkingSpace validParkingSpace(ParkingSpaceStatus status) {
        ParkingSpace parkingSpace = new ParkingSpace();
        parkingSpace.setZoneId(UUID.randomUUID());
        parkingSpace.setCode("P-01");
        parkingSpace.setStatus(status);
        parkingSpace.setLifecycleStatus("ACTIVE");
        parkingSpace.setStatusSource(com.ban.vehicle_management.shared.enumeration.parking.ParkingSpaceStatusSource.MANUAL);
        parkingSpace.setVersion(0L);
        parkingSpace.setStatusReason("Test reason");
        return parkingSpace;
    }
}

