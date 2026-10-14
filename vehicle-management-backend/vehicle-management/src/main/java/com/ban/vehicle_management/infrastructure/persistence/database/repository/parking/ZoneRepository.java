package com.ban.vehicle_management.infrastructure.persistence.database.repository.parking;

import com.ban.vehicle_management.infrastructure.persistence.database.entity.parking.ZoneEntity;
import com.ban.vehicle_management.shared.enumeration.parking.LayoutStatus;
import com.ban.vehicle_management.shared.enumeration.parking.TrackingMode;
import com.ban.vehicle_management.shared.enumeration.parking.ZoneStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ZoneRepository extends JpaRepository<ZoneEntity, UUID>, JpaSpecificationExecutor<ZoneEntity> {

    boolean existsByParkingLotIdAndCode(UUID parkingLotId, String code);

    boolean existsByParkingLotIdAndCodeAndZoneIdNot(UUID parkingLotId, String code, UUID zoneId);

    boolean existsByParkingLotIdAndStatus(UUID parkingLotId, ZoneStatus status);

    boolean existsByZoneIdAndStatus(UUID zoneId, ZoneStatus status);

    boolean existsByVehicleTypeIdsContainsAndStatus(UUID vehicleTypeId, ZoneStatus status);

    List<ZoneEntity> findByParkingLevelId(UUID parkingLevelId);

    List<ZoneEntity> findByParkingLotIdAndTrackingMode(UUID parkingLotId, TrackingMode trackingMode);

    Optional<ZoneEntity> findByParkingLotIdAndCodeAndTrackingMode(UUID parkingLotId, String code, TrackingMode trackingMode);

    @Query("""
        select coalesce(sum(zone.capacity), 0)
        from ZoneEntity zone
        join zone.vehicleTypeIds allowedVehicleTypeId
        where allowedVehicleTypeId = :vehicleTypeId
          and zone.status = com.ban.vehicle_management.shared.enumeration.parking.ZoneStatus.ACTIVE
        """)
    long sumActiveCapacityByVehicleTypeId(@Param("vehicleTypeId") UUID vehicleTypeId);

    @Query("""
        select coalesce(sum(zone.capacity), 0)
        from ZoneEntity zone join zone.vehicleTypeIds allowedVehicleTypeId
        where zone.parkingLotId = :parkingLotId
          and allowedVehicleTypeId = :vehicleTypeId
          and zone.status = com.ban.vehicle_management.shared.enumeration.parking.ZoneStatus.ACTIVE
        """)
    long sumActiveCapacityByVehicleTypeIdAndParkingLotId(UUID vehicleTypeId, UUID parkingLotId);

    long countByParkingLotIdAndTrackingModeAndLayoutStatus(UUID parkingLotId, TrackingMode trackingMode, LayoutStatus layoutStatus);
}
