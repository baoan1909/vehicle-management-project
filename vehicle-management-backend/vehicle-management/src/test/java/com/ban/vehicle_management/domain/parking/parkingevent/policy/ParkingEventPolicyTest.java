package com.ban.vehicle_management.domain.parking.parkingevent.policy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.ban.vehicle_management.domain.parking.parkingevent.model.ParkingEvent;
import com.ban.vehicle_management.shared.enumeration.parking.ParkingEventType;
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
        parkingEvent.setLicensePlateImagePath(" /images/e1.jpg ");
        parkingEvent.setPersonImagePath(" /images/person.jpg ");
        parkingEvent.setNote(" reviewed manually ");

        parkingEventPolicy.initialize(parkingEvent);

        assertNull(parkingEvent.getLicensePlateDetected());
        assertEquals("/images/e1.jpg", parkingEvent.getLicensePlateImagePath());
        assertEquals("/images/person.jpg", parkingEvent.getPersonImagePath());
        assertEquals("reviewed manually", parkingEvent.getNote());
    }

    @Test
    void shouldRequirePlateForCheckInEvent() {
        ParkingEvent parkingEvent = validParkingEvent(ParkingEventType.CHECK_IN);

        assertThrows(BadRequestException.class, () -> parkingEventPolicy.initialize(parkingEvent));
    }

    @Test
    void shouldRejectImagePathExceedingSchemaLength() {
        ParkingEvent parkingEvent = validParkingEvent(ParkingEventType.MANUAL_REVIEW);
        parkingEvent.setLicensePlateImagePath("A".repeat(256));

        assertThrows(BadRequestException.class, () -> parkingEventPolicy.initialize(parkingEvent));
    }

    @Test
    void shouldRejectPersonImagePathExceedingSchemaLength() {
        ParkingEvent parkingEvent = validParkingEvent(ParkingEventType.MANUAL_REVIEW);
        parkingEvent.setPersonImagePath("A".repeat(256));

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

