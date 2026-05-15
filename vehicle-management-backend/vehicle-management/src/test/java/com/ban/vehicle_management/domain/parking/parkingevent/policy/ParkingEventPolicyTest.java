package com.ban.vehicle_management.domain.parking.parkingevent.policy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.ban.vehicle_management.domain.parking.parkingevent.model.ParkingEvent;
import com.ban.vehicle_management.shared.enumeration.ParkingEventType;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ParkingEventPolicyTest {

    private final ParkingEventPolicy parkingEventPolicy = new ParkingEventPolicy();

    @Test
    void shouldNormalizeOptionalFields() {
        ParkingEvent parkingEvent = validParkingEvent(ParkingEventType.MANUAL_REVIEW);
        parkingEvent.setLicensePlateDetected("   ");
        parkingEvent.setImagePath(" /images/e1.jpg ");
        parkingEvent.setNote(" reviewed manually ");

        parkingEventPolicy.initialize(parkingEvent);

        assertNull(parkingEvent.getLicensePlateDetected());
        assertEquals("/images/e1.jpg", parkingEvent.getImagePath());
        assertEquals("reviewed manually", parkingEvent.getNote());
    }

    @Test
    void shouldRequirePlateForCheckInEvent() {
        ParkingEvent parkingEvent = validParkingEvent(ParkingEventType.CHECK_IN);

        assertThrows(BadRequestException.class, () -> parkingEventPolicy.initialize(parkingEvent));
    }

    private ParkingEvent validParkingEvent(ParkingEventType eventType) {
        ParkingEvent parkingEvent = new ParkingEvent();
        parkingEvent.setParkingSessionId(UUID.randomUUID());
        parkingEvent.setLaneId(UUID.randomUUID());
        parkingEvent.setEventType(eventType);
        parkingEvent.setEventTime(Instant.parse("2026-05-15T01:00:00Z"));
        return parkingEvent;
    }
}

