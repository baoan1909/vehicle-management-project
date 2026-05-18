package com.ban.vehicle_management.domain.parking.parkingsession.policy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.ban.vehicle_management.domain.parking.parkingsession.model.ParkingSession;
import com.ban.vehicle_management.shared.enumeration.ParkingSessionStatus;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ParkingSessionPolicyTest {

    private final ParkingSessionPolicy parkingSessionPolicy = new ParkingSessionPolicy();

    @Test
    void shouldInitializeParkingSessionWithOpenStatus() {
        ParkingSession parkingSession = new ParkingSession();
        parkingSession.setCardId(UUID.randomUUID());
        parkingSession.setVehicleTypeId(UUID.randomUUID());
        parkingSession.setLicensePlateIn(" 51A-12345 ");
        parkingSession.setCheckInTime(Instant.parse("2026-05-15T01:00:00Z"));

        parkingSessionPolicy.initialize(parkingSession);

        assertEquals("51A-12345", parkingSession.getLicensePlateIn());
        assertEquals(ParkingSessionStatus.OPEN, parkingSession.getStatus());
    }

    @Test
    void shouldCloseOpenParkingSession() {
        ParkingSession parkingSession = validOpenSession();
        Instant checkOutTime = Instant.parse("2026-05-15T03:00:00Z");
        BigDecimal totalPrice = new BigDecimal("15000");

        parkingSessionPolicy.checkOut(parkingSession, checkOutTime, "51A-12345", totalPrice);

        assertEquals(ParkingSessionStatus.CLOSED, parkingSession.getStatus());
        assertEquals(checkOutTime, parkingSession.getCheckOutTime());
        assertEquals("51A-12345", parkingSession.getLicensePlateOut());
        assertEquals(totalPrice, parkingSession.getTotalPrice());
    }

    @Test
    void shouldRejectOpenSessionWithOutputFields() {
        ParkingSession parkingSession = validOpenSession();
        parkingSession.setCheckOutTime(Instant.parse("2026-05-15T03:00:00Z"));

        assertThrows(BadRequestException.class, () -> parkingSessionPolicy.validateState(parkingSession));
    }

    @Test
    void shouldMarkLostCardSession() {
        ParkingSession parkingSession = validOpenSession();
        Instant checkOutTime = Instant.parse("2026-05-15T03:00:00Z");

        parkingSessionPolicy.markLostCard(parkingSession, checkOutTime, new BigDecimal("50000"));

        assertEquals(ParkingSessionStatus.LOST_CARD, parkingSession.getStatus());
        assertEquals(checkOutTime, parkingSession.getCheckOutTime());
    }

    @Test
    void shouldRejectLicensePlateInExceedingSchemaLength() {
        ParkingSession parkingSession = new ParkingSession();
        parkingSession.setCardId(UUID.randomUUID());
        parkingSession.setVehicleTypeId(UUID.randomUUID());
        parkingSession.setLicensePlateIn("A".repeat(21));
        parkingSession.setCheckInTime(Instant.parse("2026-05-15T01:00:00Z"));

        assertThrows(BadRequestException.class, () -> parkingSessionPolicy.initialize(parkingSession));
    }

    private ParkingSession validOpenSession() {
        ParkingSession parkingSession = new ParkingSession();
        parkingSession.setCardId(UUID.randomUUID());
        parkingSession.setVehicleTypeId(UUID.randomUUID());
        parkingSession.setLicensePlateIn("51A-12345");
        parkingSession.setCheckInTime(Instant.parse("2026-05-15T01:00:00Z"));
        parkingSession.setStatus(ParkingSessionStatus.OPEN);
        return parkingSession;
    }
}

