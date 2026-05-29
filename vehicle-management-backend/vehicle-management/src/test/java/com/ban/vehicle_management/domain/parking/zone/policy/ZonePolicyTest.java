package com.ban.vehicle_management.domain.parking.zone.policy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.ban.vehicle_management.domain.parking.zone.model.Zone;
import com.ban.vehicle_management.shared.enumeration.parking.ZoneStatus;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ZonePolicyTest {

    private final ZonePolicy zonePolicy = new ZonePolicy();

    @Test
    void shouldNormalizeFieldsAndSetDefaultsWhenInitialize() {
        Zone zone = validZone();
        zone.setCode(" a1 ");
        zone.setName(" Area A1 ");
        zone.setCapacity(null);
        zone.setStatus(null);

        zonePolicy.initialize(zone);

        assertEquals("A1", zone.getCode());
        assertEquals("Area A1", zone.getName());
        assertEquals(0, zone.getCapacity());
        assertEquals(ZoneStatus.ACTIVE, zone.getStatus());
    }

    @Test
    void shouldKeepExistingStatusWhenInitialize() {
        Zone zone = validZone();
        zone.setStatus(ZoneStatus.MAINTENANCE);

        zonePolicy.initialize(zone);

        assertEquals(ZoneStatus.MAINTENANCE, zone.getStatus());
    }

    @Test
    void shouldRejectNullZone() {
        assertThrows(BadRequestException.class, () -> zonePolicy.initialize(null));
    }

    @Test
    void shouldRejectNullParkingLotId() {
        Zone zone = validZone();
        zone.setParkingLotId(null);

        assertThrows(BadRequestException.class, () -> zonePolicy.initialize(zone));
    }

    @Test
    void shouldRejectBlankCode() {
        Zone zone = validZone();
        zone.setCode(" ");

        assertThrows(BadRequestException.class, () -> zonePolicy.initialize(zone));
    }

    @Test
    void shouldRejectBlankName() {
        Zone zone = validZone();
        zone.setName(" ");

        assertThrows(BadRequestException.class, () -> zonePolicy.initialize(zone));
    }

    @Test
    void shouldRejectNegativeCapacity() {
        Zone zone = validZone();
        zone.setCapacity(-10);

        assertThrows(BadRequestException.class, () -> zonePolicy.validateState(zone));
    }

    @Test
    void shouldRejectZoneNameExceedingSchemaLength() {
        Zone zone = validZone();
        zone.setName("A".repeat(151));

        assertThrows(BadRequestException.class, () -> zonePolicy.initialize(zone));
    }

    @Test
    void shouldActivateZone() {
        Zone zone = validZone();
        zone.setStatus(ZoneStatus.CLOSED);

        zonePolicy.activate(zone);

        assertEquals(ZoneStatus.ACTIVE, zone.getStatus());
    }

    @Test
    void shouldMarkZoneMaintenance() {
        Zone zone = validZone();

        zonePolicy.markMaintenance(zone);

        assertEquals(ZoneStatus.MAINTENANCE, zone.getStatus());
    }

    @Test
    void shouldCloseZone() {
        Zone zone = validZone();

        zonePolicy.close(zone);

        assertEquals(ZoneStatus.CLOSED, zone.getStatus());
    }

    @Test
    void shouldAllowNullVehicleTypeId() {
        Zone zone = validZone();
        zone.setVehicleTypeId(null);

        zonePolicy.initialize(zone);

        assertNull(zone.getVehicleTypeId());
    }

    private Zone validZone() {
        Zone zone = new Zone();
        zone.setParkingLotId(UUID.randomUUID());
        zone.setVehicleTypeId(UUID.randomUUID());
        zone.setCode("A1");
        zone.setName("Area A1");
        zone.setCapacity(100);
        zone.setStatus(ZoneStatus.ACTIVE);
        return zone;
    }
}

