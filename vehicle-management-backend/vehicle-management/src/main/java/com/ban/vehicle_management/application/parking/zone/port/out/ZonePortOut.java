package com.ban.vehicle_management.application.parking.zone.port.out;

import com.ban.vehicle_management.domain.parking.zone.model.Zone;
import com.ban.vehicle_management.shared.enumeration.parking.ZoneStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ZonePortOut {
    Zone save(Zone zone);

    Optional<Zone> findById(UUID zoneId);

    List<Zone> findAll(UUID parkingLotId, UUID vehicleTypeId, ZoneStatus status, String keyword);

    boolean existsByParkingLotIdAndCode(UUID parkingLotId, String code);

    boolean existsByParkingLotIdAndCodeAndZoneIdNot(UUID parkingLotId, String code, UUID zoneId);

    boolean existsActiveParkingLotById(UUID parkingLotId);

    boolean existsActiveVehicleTypeById(UUID vehicleTypeId);

    long countOpenSessions(UUID zoneId);

    boolean hasOpenSessions(UUID zoneId);

    boolean hasActiveGates(UUID zoneId);
}