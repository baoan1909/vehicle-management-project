package com.ban.vehicle_management.infrastructure.persistence.database.repository.parking;

import com.ban.vehicle_management.infrastructure.persistence.database.entity.parking.ZoneEntity;
import java.util.UUID;

import com.ban.vehicle_management.shared.enumeration.parking.ZoneStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ZoneRepository extends JpaRepository<ZoneEntity, UUID>, JpaSpecificationExecutor<ZoneEntity> {

    boolean existsByParkingLotIdAndCode(UUID parkingLotId, String code);

    boolean existsByParkingLotIdAndCodeAndZoneIdNot(UUID parkingLotId, String code, UUID zoneId);

    boolean existsByParkingLotIdAndStatus(UUID parkingLotId, ZoneStatus status);

    boolean existsByZoneIdAndStatus(UUID zoneId, ZoneStatus status);

    boolean existsByVehicleTypeIdAndStatus(UUID vehicleTypeId, ZoneStatus status);

    @Query("""
        select coalesce(sum(zone.capacity), 0)
        from ZoneEntity zone
        where zone.vehicleTypeId = :vehicleTypeId
          and zone.status = com.ban.vehicle_management.shared.enumeration.parking.ZoneStatus.ACTIVE
        """)
    long sumActiveCapacityByVehicleTypeId(@Param("vehicleTypeId") UUID vehicleTypeId);
}
