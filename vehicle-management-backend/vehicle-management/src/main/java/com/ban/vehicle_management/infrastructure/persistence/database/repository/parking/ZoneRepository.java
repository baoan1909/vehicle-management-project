package com.ban.vehicle_management.infrastructure.persistence.database.repository.parking;

import com.ban.vehicle_management.infrastructure.persistence.database.entity.parking.ZoneEntity;
import com.ban.vehicle_management.shared.enumeration.parking.LayoutStatus;
import com.ban.vehicle_management.shared.enumeration.parking.TrackingMode;
import com.ban.vehicle_management.shared.enumeration.parking.ZoneStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;
import java.util.Collection;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ZoneRepository extends JpaRepository<ZoneEntity, UUID>, JpaSpecificationExecutor<ZoneEntity> {

    @Query("""
            select coalesce(sum(zone.capacity), 0)
            from ZoneEntity zone
            where zone.status = com.ban.vehicle_management.shared.enumeration.parking.ZoneStatus.ACTIVE
            """)
    long sumDashboardActiveCapacity();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select zone from ZoneEntity zone where zone.zoneId = :zoneId")
    Optional<ZoneEntity> findByIdForUpdate(@Param("zoneId") UUID zoneId);

    boolean existsByParkingLotIdAndCode(UUID parkingLotId, String code);

    boolean existsByParkingLotIdAndCodeAndZoneIdNot(UUID parkingLotId, String code, UUID zoneId);

    boolean existsByParkingLotIdAndStatus(UUID parkingLotId, ZoneStatus status);

    boolean existsByParkingLotIdAndStatusAndTrackingModeAndLayoutStatusNot(
            UUID parkingLotId,
            ZoneStatus status,
            TrackingMode trackingMode,
            LayoutStatus layoutStatus
    );

    boolean existsByZoneIdAndStatus(UUID zoneId, ZoneStatus status);

    boolean existsByVehicleTypeIdsContainsAndStatus(UUID vehicleTypeId, ZoneStatus status);

    long countByVehicleTypeIdsContains(UUID vehicleTypeId);

    List<ZoneEntity> findByVehicleTypeIdsContains(UUID vehicleTypeId);

    List<ZoneEntity> findByParkingLotIdAndVehicleTypeIdsContains(UUID parkingLotId, UUID vehicleTypeId);

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

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
        update parking.zones zone set status = 'CLOSED', updated_at = :now
          from parking.parking_lots lot
         where lot.parking_lot_id = zone.parking_lot_id
           and lot.organization_id in (:organizationIds) and zone.status <> 'CLOSED'
        """, nativeQuery = true)
    int closeAllByOrganizationIds(
            @Param("organizationIds") Collection<UUID> organizationIds,
            @Param("now") java.time.Instant now);
}
