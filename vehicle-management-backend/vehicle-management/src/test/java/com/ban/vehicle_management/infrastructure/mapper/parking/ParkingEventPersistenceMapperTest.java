package com.ban.vehicle_management.infrastructure.mapper.parking;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.ban.vehicle_management.domain.parking.parkingevent.model.ParkingEvent;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.parking.ParkingEventEntity;
import com.ban.vehicle_management.shared.enumeration.parking.ParkingEventType;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

class ParkingEventPersistenceMapperTest {

    private final ParkingEventPersistenceMapper mapper =
            Mappers.getMapper(ParkingEventPersistenceMapper.class);

    @Test
    void shouldRoundTripVehicleEvidence() {
        ParkingEvent event = new ParkingEvent();
        event.setParkingEventId(UUID.randomUUID());
        event.setParkingSessionId(UUID.randomUUID());
        event.setLaneId(UUID.randomUUID());
        event.setEventType(ParkingEventType.CHECK_OUT);
        event.setEventTime(Instant.parse("2026-11-10T08:00:00Z"));
        event.setLicensePlateDetected("51G49524");
        event.setLicensePlateImagePath("parking/license-plate.jpg");
        event.setVehicleImagePath("parking/vehicle.jpg");
        event.setPersonImagePath("parking/person.jpg");

        ParkingEventEntity entity = mapper.toEntity(event);
        entity.setLicensePlateDetectedNormalized(event.getLicensePlateDetected());
        ParkingEvent restored = mapper.toDomain(entity);

        assertEquals("parking/vehicle.jpg", entity.getVehicleImagePath());
        assertEquals("parking/vehicle.jpg", restored.getVehicleImagePath());
    }

    @Test
    void shouldReadLegacyEventWithoutVehicleEvidence() {
        ParkingEventEntity legacy = new ParkingEventEntity();
        legacy.setParkingEventId(UUID.randomUUID());
        legacy.setParkingSessionId(UUID.randomUUID());
        legacy.setEventType(ParkingEventType.CHECK_IN);
        legacy.setEventTime(Instant.parse("2026-01-01T00:00:00Z"));

        ParkingEvent restored = mapper.toDomain(legacy);

        assertNull(restored.getVehicleImagePath());
    }
}
