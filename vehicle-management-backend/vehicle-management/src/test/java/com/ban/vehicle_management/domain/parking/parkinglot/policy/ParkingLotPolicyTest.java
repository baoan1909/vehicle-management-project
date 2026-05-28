package com.ban.vehicle_management.domain.parking.parkinglot.policy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.ban.vehicle_management.domain.parking.parkinglot.model.ParkingLot;
import com.ban.vehicle_management.shared.enumeration.parking.ParkingLotStatus;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import org.junit.jupiter.api.Test;

class ParkingLotPolicyTest {

    private final ParkingLotPolicy parkingLotPolicy = new ParkingLotPolicy();

    @Test
    void shouldNormalizeFieldsAndSetDefaultsWhenInitialize() {
        ParkingLot parkingLot = validParkingLot();
        parkingLot.setCode(" hcmute ");
        parkingLot.setName(" Bai xe HCMUTE ");
        parkingLot.setAddress(" So 1 Vo Van Ngan ");
        parkingLot.setTotalCapacity(null);
        parkingLot.setStatus(null);

        parkingLotPolicy.initialize(parkingLot);

        assertEquals("HCMUTE", parkingLot.getCode());
        assertEquals("Bai xe HCMUTE", parkingLot.getName());
        assertEquals("So 1 Vo Van Ngan", parkingLot.getAddress());
        assertEquals(0, parkingLot.getTotalCapacity());
        assertEquals(ParkingLotStatus.ACTIVE, parkingLot.getStatus());
    }

    @Test
    void shouldKeepExistingStatusWhenInitialize() {
        ParkingLot parkingLot = validParkingLot();
        parkingLot.setStatus(ParkingLotStatus.MAINTENANCE);

        parkingLotPolicy.initialize(parkingLot);

        assertEquals(ParkingLotStatus.MAINTENANCE, parkingLot.getStatus());
    }

    @Test
    void shouldRejectNullParkingLot() {
        assertThrows(BadRequestException.class, () -> parkingLotPolicy.initialize(null));
    }

    @Test
    void shouldRejectBlankCode() {
        ParkingLot parkingLot = validParkingLot();
        parkingLot.setCode(" ");

        assertThrows(BadRequestException.class, () -> parkingLotPolicy.initialize(parkingLot));
    }

    @Test
    void shouldRejectBlankName() {
        ParkingLot parkingLot = validParkingLot();
        parkingLot.setName(" ");

        assertThrows(BadRequestException.class, () -> parkingLotPolicy.initialize(parkingLot));
    }

    @Test
    void shouldRejectNegativeTotalCapacity() {
        ParkingLot parkingLot = validParkingLot();
        parkingLot.setTotalCapacity(-1);

        assertThrows(BadRequestException.class, () -> parkingLotPolicy.validateState(parkingLot));
    }

    @Test
    void shouldRejectNameExceedingSchemaLength() {
        ParkingLot parkingLot = validParkingLot();
        parkingLot.setName("A".repeat(151));

        assertThrows(BadRequestException.class, () -> parkingLotPolicy.initialize(parkingLot));
    }

    @Test
    void shouldActivateParkingLot() {
        ParkingLot parkingLot = validParkingLot();
        parkingLot.setStatus(ParkingLotStatus.CLOSED);

        parkingLotPolicy.activate(parkingLot);

        assertEquals(ParkingLotStatus.ACTIVE, parkingLot.getStatus());
    }

    @Test
    void shouldMarkParkingLotMaintenance() {
        ParkingLot parkingLot = validParkingLot();

        parkingLotPolicy.markMaintenance(parkingLot);

        assertEquals(ParkingLotStatus.MAINTENANCE, parkingLot.getStatus());
    }

    @Test
    void shouldCloseParkingLot() {
        ParkingLot parkingLot = validParkingLot();

        parkingLotPolicy.close(parkingLot);

        assertEquals(ParkingLotStatus.CLOSED, parkingLot.getStatus());
    }

    @Test
    void shouldNormalizeAddressToNullWhenBlank() {
        ParkingLot parkingLot = validParkingLot();
        parkingLot.setAddress(" ");

        parkingLotPolicy.initialize(parkingLot);

        assertTrue(parkingLot.getAddress() == null);
    }

    private ParkingLot validParkingLot() {
        ParkingLot parkingLot = new ParkingLot();
        parkingLot.setCode("HCMUTE");
        parkingLot.setName("Bai xe HCMUTE");
        parkingLot.setAddress("So 1 Vo Van Ngan");
        parkingLot.setTotalCapacity(1000);
        parkingLot.setStatus(ParkingLotStatus.ACTIVE);
        return parkingLot;
    }
}

