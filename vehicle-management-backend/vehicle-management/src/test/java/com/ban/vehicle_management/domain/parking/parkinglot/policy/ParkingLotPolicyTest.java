package com.ban.vehicle_management.domain.parking.parkinglot.policy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.ban.vehicle_management.domain.parking.parkinglot.model.ParkingLot;
import com.ban.vehicle_management.shared.enumeration.parking.ParkingLotStatus;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import org.junit.jupiter.api.Test;

class ParkingLotPolicyTest {

    private final ParkingLotPolicy parkingLotPolicy = new ParkingLotPolicy();

    @Test
    void shouldInitializeParkingLotWithDefaults() {
        ParkingLot parkingLot = new ParkingLot();
        parkingLot.setCode(" LOT-01 ");
        parkingLot.setName(" Main Lot ");

        parkingLotPolicy.initialize(parkingLot);

        assertEquals("LOT-01", parkingLot.getCode());
        assertEquals("Main Lot", parkingLot.getName());
        assertEquals(0, parkingLot.getTotalCapacity());
        assertEquals(ParkingLotStatus.ACTIVE, parkingLot.getStatus());
    }

    @Test
    void shouldRejectNegativeCapacity() {
        ParkingLot parkingLot = new ParkingLot();
        parkingLot.setCode("LOT-01");
        parkingLot.setName("Main Lot");
        parkingLot.setTotalCapacity(-1);
        parkingLot.setStatus(ParkingLotStatus.ACTIVE);

        assertThrows(BadRequestException.class, () -> parkingLotPolicy.validateState(parkingLot));
    }

    @Test
    void shouldRejectParkingLotNameExceedingSchemaLength() {
        ParkingLot parkingLot = new ParkingLot();
        parkingLot.setCode("LOT-01");
        parkingLot.setName("A".repeat(151));

        assertThrows(BadRequestException.class, () -> parkingLotPolicy.initialize(parkingLot));
    }
}

