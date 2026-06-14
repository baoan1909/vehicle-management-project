package com.ban.vehicle_management.infrastructure.persistence.database.repository.parking;

import com.ban.vehicle_management.infrastructure.persistence.database.entity.parking.ZoneEntity;
import java.util.UUID;

import com.ban.vehicle_management.shared.enumeration.parking.ZoneStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ZoneRepository extends JpaRepository<ZoneEntity, UUID>, JpaSpecificationExecutor<ZoneEntity> {

    boolean existsByParkingLotIdAndCode(UUID parkingLotId, String code);

    boolean existsByParkingLotIdAndCodeAndZoneIdNot(UUID parkingLotId, String code, UUID zoneId);

    boolean existsByParkingLotIdAndStatus(UUID parkingLotId, ZoneStatus status);

    boolean existsByZoneIdAndStatus(UUID zoneId, ZoneStatus status);

    boolean existsByVehicleTypeIdAndStatus(UUID vehicleTypeId, ZoneStatus status);
}
