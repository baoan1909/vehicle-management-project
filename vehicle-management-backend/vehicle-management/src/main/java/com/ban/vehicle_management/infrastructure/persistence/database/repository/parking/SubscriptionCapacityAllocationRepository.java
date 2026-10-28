package com.ban.vehicle_management.infrastructure.persistence.database.repository.parking;

import com.ban.vehicle_management.infrastructure.persistence.database.entity.parking.SubscriptionCapacityAllocationEntity;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

public interface SubscriptionCapacityAllocationRepository
        extends JpaRepository<SubscriptionCapacityAllocationEntity, UUID> {

    @Query("""
        select a from SubscriptionCapacityAllocationEntity a
        where a.subscriptionId = :subscriptionId
          and a.allocationStatus in (
            com.ban.vehicle_management.shared.enumeration.parking.SubscriptionCapacityAllocationStatus.HELD,
            com.ban.vehicle_management.shared.enumeration.parking.SubscriptionCapacityAllocationStatus.ACTIVE)
        """)
    Optional<SubscriptionCapacityAllocationEntity> findLiveBySubscriptionId(UUID subscriptionId);

    @Query("""
        select a from SubscriptionCapacityAllocationEntity a
        where a.subscriptionId = :subscriptionId
          and a.effectiveFrom <= :date and a.effectiveTo >= :date
          and (a.allocationStatus = com.ban.vehicle_management.shared.enumeration.parking.SubscriptionCapacityAllocationStatus.ACTIVE
            or (a.allocationStatus = com.ban.vehicle_management.shared.enumeration.parking.SubscriptionCapacityAllocationStatus.HELD
              and a.holdExpiresAt > CURRENT_TIMESTAMP))
        """)
    Optional<SubscriptionCapacityAllocationEntity> findEffectiveBySubscriptionId(UUID subscriptionId, LocalDate date);

    @Query("""
        select a from SubscriptionCapacityAllocationEntity a
        where a.zoneId = :zoneId and a.effectiveFrom <= :date and a.effectiveTo >= :date
          and (a.allocationStatus = com.ban.vehicle_management.shared.enumeration.parking.SubscriptionCapacityAllocationStatus.ACTIVE
            or (a.allocationStatus = com.ban.vehicle_management.shared.enumeration.parking.SubscriptionCapacityAllocationStatus.HELD
              and a.holdExpiresAt > CURRENT_TIMESTAMP))
        """)
    List<SubscriptionCapacityAllocationEntity> findEffectiveByZoneId(UUID zoneId, LocalDate date);

    @Query(value = """
        SELECT COALESCE(MAX(day_count), 0) FROM (
          SELECT d.day, COUNT(a.allocation_id) AS day_count
          FROM generate_series(CAST(:effectiveFrom AS date), CAST(:effectiveTo AS date), interval '1 day') d(day)
          LEFT JOIN parking.subscription_capacity_allocations a
            ON a.zone_id = :zoneId
           AND a.allocation_status IN ('HELD', 'ACTIVE')
           AND (a.allocation_status <> 'HELD' OR a.hold_expires_at > now())
           AND a.effective_from <= d.day::date AND a.effective_to >= d.day::date
           AND (CAST(:excludedAllocationId AS uuid) IS NULL OR a.allocation_id <> CAST(:excludedAllocationId AS uuid))
          GROUP BY d.day
        ) counts
        """, nativeQuery = true)
    long maxConcurrentReservations(@Param("zoneId") UUID zoneId,
            @Param("effectiveFrom") LocalDate effectiveFrom,
            @Param("effectiveTo") LocalDate effectiveTo,
            @Param("excludedAllocationId") UUID excludedAllocationId);

    @Query(value = """
        SELECT COALESCE(MAX((
          SELECT COUNT(*)
          FROM parking.subscription_capacity_allocations concurrent
          WHERE concurrent.zone_id = :zoneId
            AND concurrent.allocation_status IN ('HELD', 'ACTIVE')
            AND (concurrent.allocation_status <> 'HELD' OR concurrent.hold_expires_at > now())
            AND concurrent.effective_from <= boundary.day
            AND concurrent.effective_to >= boundary.day
        )), 0)
        FROM (
          SELECT DISTINCT effective_from AS day
          FROM parking.subscription_capacity_allocations
          WHERE zone_id = :zoneId
            AND allocation_status IN ('HELD', 'ACTIVE')
            AND (allocation_status <> 'HELD' OR hold_expires_at > now())
        ) boundary
        """, nativeQuery = true)
    long maxConcurrentReservations(@Param("zoneId") UUID zoneId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select a from SubscriptionCapacityAllocationEntity a
        where a.allocationStatus = com.ban.vehicle_management.shared.enumeration.parking.SubscriptionCapacityAllocationStatus.HELD
          and a.holdExpiresAt <= :now
        """)
    List<SubscriptionCapacityAllocationEntity> findExpiredHolds(Instant now);
}

