package com.ban.vehicle_management.infrastructure.persistence.database.repository.parking;

import com.ban.vehicle_management.infrastructure.persistence.database.entity.parking.ParkingSpaceAllocationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;
import java.time.LocalDate;
import java.util.UUID;

public interface ParkingSpaceAllocationRepository extends JpaRepository<ParkingSpaceAllocationEntity, UUID> {

    List<ParkingSpaceAllocationEntity> findByParkingSpaceId(UUID parkingSpaceId);

    List<ParkingSpaceAllocationEntity> findBySubscriptionId(UUID subscriptionId);

    List<ParkingSpaceAllocationEntity> findByParkingSpaceIdAndAllocationStatus(UUID parkingSpaceId, String allocationStatus);

    @Query("SELECT psa FROM ParkingSpaceAllocationEntity psa WHERE psa.parkingSpaceId = :parkingSpaceId AND psa.allocationStatus IN ('HELD', 'ACTIVE') AND psa.effectiveFrom <= :date AND psa.effectiveTo >= :date")
    List<ParkingSpaceAllocationEntity> findActiveAllocationsForSpaceOnDate(UUID parkingSpaceId, LocalDate date);

    @Query("SELECT psa FROM ParkingSpaceAllocationEntity psa WHERE psa.subscriptionId = :subscriptionId AND psa.allocationStatus IN ('HELD', 'ACTIVE') ORDER BY psa.effectiveFrom")
    List<ParkingSpaceAllocationEntity> findActiveAllocationsBySubscription(UUID subscriptionId);

    @Query("SELECT psa FROM ParkingSpaceAllocationEntity psa WHERE psa.parkingSpaceId = :parkingSpaceId AND psa.allocationStatus = 'HELD' AND psa.holdExpiresAt < :now")
    List<ParkingSpaceAllocationEntity> findExpiredHolds(UUID parkingSpaceId, java.time.Instant now);

    boolean existsByParkingSpaceIdAndEffectiveFromAndEffectiveTo(UUID parkingSpaceId, LocalDate effectiveFrom, LocalDate effectiveTo);

    Optional<ParkingSpaceAllocationEntity> findByParkingSpaceIdAndSubscriptionIdAndEffectiveFromAndEffectiveTo(UUID parkingSpaceId, UUID subscriptionId, LocalDate effectiveFrom, LocalDate effectiveTo);

    Optional<ParkingSpaceAllocationEntity> findByParkingSpaceIdAndEffectiveFromAndEffectiveTo(UUID parkingSpaceId, LocalDate effectiveFrom, LocalDate effectiveTo);

    @Query("""
        SELECT CASE WHEN COUNT(psa) > 0 THEN TRUE ELSE FALSE END
        FROM ParkingSpaceAllocationEntity psa
        WHERE psa.parkingSpaceId = :parkingSpaceId
          AND psa.allocationStatus IN (
            com.ban.vehicle_management.shared.enumeration.parking.ParkingSpaceAllocationStatus.HELD,
            com.ban.vehicle_management.shared.enumeration.parking.ParkingSpaceAllocationStatus.ACTIVE)
          AND (:excludeAllocationId IS NULL OR psa.allocationId <> :excludeAllocationId)
          AND psa.effectiveFrom <= :effectiveTo
          AND psa.effectiveTo >= :effectiveFrom
        """)
    boolean existsOverlappingActiveAllocation(
            @Param("parkingSpaceId") UUID parkingSpaceId,
            @Param("effectiveFrom") LocalDate effectiveFrom,
            @Param("effectiveTo") LocalDate effectiveTo,
            @Param("excludeAllocationId") UUID excludeAllocationId);
}