package com.ban.vehicle_management.domain.parking.zone.policy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.ban.vehicle_management.domain.parking.zone.model.Zone;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ZonePolicyTest {

    private final ZonePolicy zonePolicy = new ZonePolicy();

    @Test
    void shouldInitializeZoneWithDefaults() {
        Zone zone = new Zone();
        zone.setParkingLotId(UUID.randomUUID());
        zone.setCode(" A1 ");
        zone.setName(" Area A1 ");

        zonePolicy.initialize(zone);

        assertEquals("A1", zone.getCode());
        assertEquals("Area A1", zone.getName());
        assertEquals(0, zone.getCapacity());
    }

    @Test
    void shouldRejectNegativeCapacity() {
        Zone zone = new Zone();
        zone.setParkingLotId(UUID.randomUUID());
        zone.setCode("A1");
        zone.setName("Area A1");
        zone.setCapacity(-10);

        assertThrows(BadRequestException.class, () -> zonePolicy.validateState(zone));
    }

    @Test
    void shouldRejectZoneNameExceedingSchemaLength() {
        Zone zone = new Zone();
        zone.setParkingLotId(UUID.randomUUID());
        zone.setCode("A1");
        zone.setName("A".repeat(151));

        assertThrows(BadRequestException.class, () -> zonePolicy.initialize(zone));
    }
}

