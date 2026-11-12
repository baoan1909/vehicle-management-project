package com.ban.vehicle_management.infrastructure.persistence.database.repository.parking;

import com.ban.vehicle_management.infrastructure.persistence.database.entity.parking.ParkingSpaceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;
import java.util.Collection;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ParkingSpaceRepository extends JpaRepository<ParkingSpaceEntity, UUID> {

    @Query(value = "select count(*) from parking.parking_spaces where lifecycle_status='ACTIVE'",
            nativeQuery = true)
    long countActiveForMetrics();

    @Query(value = """
        select count(*) from parking.parking_spaces
        where lifecycle_status='ACTIVE' and status='OCCUPIED'
        """, nativeQuery = true)
    long countOccupiedForMetrics();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select space from ParkingSpaceEntity space where space.parkingSpaceId = :parkingSpaceId")
    Optional<ParkingSpaceEntity> findByIdForUpdate(@Param("parkingSpaceId") UUID parkingSpaceId);

    @Query(value = """
            SELECT space.*
            FROM parking.parking_spaces space
            WHERE space.zone_id = :zoneId
              AND space.lifecycle_status = 'ACTIVE'
              AND space.status = 'AVAILABLE'
              AND (space.vehicle_type_id IS NULL OR space.vehicle_type_id = :vehicleTypeId)
              AND NOT EXISTS (
                  SELECT 1
                  FROM parking.parking_sessions session
                  WHERE session.parking_space_id = space.parking_space_id
                    AND session.status = 'OPEN'
              )
              AND NOT EXISTS (
                  SELECT 1
                  FROM parking.parking_space_allocations allocation
                  WHERE allocation.parking_space_id = space.parking_space_id
                    AND (
                        allocation.allocation_status = 'ACTIVE'
                        OR (allocation.allocation_status = 'HELD'
                            AND allocation.hold_expires_at IS NOT NULL
                            AND allocation.hold_expires_at > :now)
                    )
              )
              AND NOT EXISTS (
                  SELECT 1
                  FROM parking.customer_reservations reservation
                  WHERE reservation.parking_space_id = space.parking_space_id
                    AND reservation.status IN ('PENDING_PAYMENT', 'HELD')
                    AND (reservation.status <> 'PENDING_PAYMENT'
                         OR reservation.payment_hold_expires_at > :now)
              )
              AND NOT EXISTS (
                  SELECT 1
                  FROM parking.check_in_space_holds hold
                  WHERE hold.parking_space_id = space.parking_space_id
                    AND hold.status = 'HELD'
                    AND hold.expires_at > :now
              )
            ORDER BY space.code, space.parking_space_id
            LIMIT 1
            FOR UPDATE OF space SKIP LOCKED
            """, nativeQuery = true)
    Optional<ParkingSpaceEntity> findFirstVisitorAvailableForUpdate(
            @Param("zoneId") UUID zoneId,
            @Param("vehicleTypeId") UUID vehicleTypeId,
            @Param("now") Instant now
    );

    List<ParkingSpaceEntity> findByZoneId(UUID zoneId);

    Optional<ParkingSpaceEntity> findByZoneIdAndCode(UUID zoneId, String code);

    List<ParkingSpaceEntity> findByZoneIdAndLifecycleStatus(UUID zoneId, String lifecycleStatus);

    List<ParkingSpaceEntity> findByZoneIdAndStatus(UUID zoneId, com.ban.vehicle_management.shared.enumeration.parking.ParkingSpaceStatus status);

    @Query("SELECT ps FROM ParkingSpaceEntity ps WHERE ps.zoneId = :zoneId AND ps.status = 'OCCUPIED' AND ps.manualOccupancyType = 'REGISTERED'")
    List<ParkingSpaceEntity> findOccupiedByRegistered(UUID zoneId);

    @Query("SELECT ps FROM ParkingSpaceEntity ps WHERE ps.zoneId = :zoneId AND ps.status = 'OCCUPIED' AND ps.manualOccupancyType = 'VISITOR'")
    List<ParkingSpaceEntity> findOccupiedByVisitor(UUID zoneId);

    @Query("SELECT ps FROM ParkingSpaceEntity ps WHERE ps.zoneId = :zoneId AND ps.status = 'RESERVED'")
    List<ParkingSpaceEntity> findReserved(UUID zoneId);

    @Query("SELECT ps FROM ParkingSpaceEntity ps WHERE ps.zoneId = :zoneId AND ps.status = 'AVAILABLE'")
    List<ParkingSpaceEntity> findAvailable(UUID zoneId);

    @Query("SELECT ps FROM ParkingSpaceEntity ps WHERE ps.zoneId = :zoneId AND ps.status = 'MAINTENANCE'")
    List<ParkingSpaceEntity> findMaintenance(UUID zoneId);

    boolean existsByZoneIdAndCode(UUID zoneId, String code);

    boolean existsByZoneIdAndCodeAndParkingSpaceIdNot(UUID zoneId, String code, UUID parkingSpaceId);

    long countByZoneIdAndStatus(UUID zoneId, com.ban.vehicle_management.shared.enumeration.parking.ParkingSpaceStatus status);

    long countByZoneIdAndLifecycleStatus(UUID zoneId, String lifecycleStatus);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
        update parking.parking_spaces space
           set lifecycle_status = 'ARCHIVED', updated_at = :now, version = version + 1
          from parking.zones zone
          join parking.parking_lots lot on lot.parking_lot_id = zone.parking_lot_id
         where space.zone_id = zone.zone_id
           and lot.organization_id in (:organizationIds) and space.lifecycle_status <> 'ARCHIVED'
        """, nativeQuery = true)
    int archiveAllByOrganizationIds(
            @Param("organizationIds") Collection<UUID> organizationIds,
            @Param("now") Instant now);
}
