package com.ban.vehicle_management.infrastructure.persistence.database.repository.operations;

import com.ban.vehicle_management.infrastructure.persistence.database.entity.operations.ShiftEntity;
import com.ban.vehicle_management.shared.enumeration.operations.ShiftStatus;
import com.ban.vehicle_management.infrastructure.persistence.database.projection.iam.AccountDeletionParkingResourceProjection;
import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.query.Param;
import com.ban.vehicle_management.infrastructure.persistence.database.projection.iam.AccountDeletionResourceScopeProjection;
import java.util.Collection;

public interface ShiftRepository
        extends JpaRepository<ShiftEntity, UUID>,
        JpaSpecificationExecutor<ShiftEntity> {

    @Query(value = """
        select shift.shift_id as "resourceId", lot.organization_id as "organizationId",
               shift.parking_lot_id as "parkingLotId"
        from operations.shifts shift
        join parking.parking_lots lot on lot.parking_lot_id=shift.parking_lot_id
        where shift.opened_by=:accountId and shift.status='OPEN'
        """, nativeQuery = true)
    List<AccountDeletionParkingResourceProjection> findDeletionBlockingResponsibilities(
            @Param("accountId") UUID accountId);

    @Query(value = """
        select shift.shift_id as "resourceId", lot.organization_id as "organizationId",
               lot.parking_lot_id as "parkingLotId"
          from operations.shifts shift
          join parking.parking_lots lot on lot.parking_lot_id = shift.parking_lot_id
         where lot.organization_id in (:organizationIds) and shift.status = 'OPEN'
        """, nativeQuery = true)
    List<AccountDeletionResourceScopeProjection> findOrganizationDeletionBlockers(
            @Param("organizationIds") Collection<UUID> organizationIds);

    boolean existsByShiftCode(String shiftCode);

    boolean existsByParkingLotIdAndShiftDateBetween(
            UUID parkingLotId,
            LocalDate fromDate,
            LocalDate toDate
    );

    boolean existsByParkingLotIdAndStatus(
            UUID parkingLotId,
            ShiftStatus status
    );

    List<ShiftEntity>
    findAllByParkingLotIdAndShiftDateBetweenOrderByStartTimeAsc(
            UUID parkingLotId,
            LocalDate fromDate,
            LocalDate toDate
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT shiftItem
            FROM ShiftEntity shiftItem
            WHERE shiftItem.shiftId = :shiftId
            """)
    Optional<ShiftEntity> findByIdForUpdate(
            @Param("shiftId") UUID shiftId
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT shiftItem
        FROM ShiftEntity shiftItem
        WHERE shiftItem.parkingLotId = :parkingLotId
          AND shiftItem.shiftDate BETWEEN :fromDate AND :toDate
        ORDER BY shiftItem.startTime ASC
        """)
    List<ShiftEntity> findAllByWeekForUpdate(
            @Param("parkingLotId") UUID parkingLotId,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
        update operations.shifts shift
           set status = 'CANCELLED', cancelled_at = :now,
               cancellation_reason = 'ORGANIZATION_CLOSED', updated_at = :now
          from parking.parking_lots lot
         where lot.parking_lot_id = shift.parking_lot_id
           and lot.organization_id in (:organizationIds) and shift.status in ('DRAFT', 'SCHEDULED')
        """, nativeQuery = true)
    int cancelFutureByOrganizationIds(
            @Param("organizationIds") Collection<UUID> organizationIds,
            @Param("now") java.time.Instant now);
}
