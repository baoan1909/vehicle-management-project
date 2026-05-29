package com.ban.vehicle_management.application.parking.zone.port.in;

import com.ban.vehicle_management.domain.parking.zone.model.Zone;
import com.ban.vehicle_management.shared.enumeration.parking.ZoneStatus;
import java.util.List;
import java.util.UUID;

public interface ZonePortIn {
    Zone createZone(Zone zone);

    Zone getZoneById(UUID zoneId);

    List<Zone> getZones(UUID parkingLotId, UUID vehicleTypeId, ZoneStatus status, String keyword);

    Zone updateZone(UUID zoneId, Zone zone);

    void deleteZone(UUID zoneId);

    Zone activateZone(UUID zoneId);

    Zone markZoneMaintenance(UUID zoneId);

    Zone closeZone(UUID zoneId);
}