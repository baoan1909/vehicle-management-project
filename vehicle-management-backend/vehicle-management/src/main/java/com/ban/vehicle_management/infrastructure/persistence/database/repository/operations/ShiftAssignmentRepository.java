package com.ban.vehicle_management.infrastructure.persistence.database.repository.operations;

import com.ban.vehicle_management.infrastructure.persistence.database.entity.operations.ShiftAssignmentEntity;
import com.ban.vehicle_management.shared.enumeration.operations.ShiftAssignmentStatus;
import com.ban.vehicle_management.shared.enumeration.operations.ShiftType;
import com.ban.vehicle_management.infrastructure.persistence.database.projection.iam.AccountDeletionShiftAssignmentProjection;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.query.Param;

public interface ShiftAssignmentRepository
        extends JpaRepository<ShiftAssignmentEntity, UUID>,
        JpaSpecificationExecutor<ShiftAssignmentEntity> {

    @Query(value = """
        select assignment.shift_assignment_id as "assignmentId",
               lot.organization_id as "organizationId",
               shift.parking_lot_id as "parkingLotId", shift.status as "shiftStatus"
        from operations.shift_assignments assignment
        join operations.shifts shift on shift.shift_id=assignment.shift_id
        join parking.parking_lots lot on lot.parking_lot_id=shift.parking_lot_id
        where assignment.employee_id=:employeeId and assignment.status in ('DRAFT','SCHEDULED','ACTIVE')
          and shift.status not in ('CLOSED','CANCELLED')
        """, nativeQuery = true)
    List<AccountDeletionShiftAssignmentProjection> findDeletionBlockingAssignments(
            @Param("employeeId") UUID employeeId);

    @Query("""
            SELECT DISTINCT assignment.shift.parkingLotId
            FROM ShiftAssignmentEntity assignment, AccountEntity account
            WHERE account.accountId = :accountId
              AND assignment.employee.userProfileId = account.userProfileId
              AND assignment.status = com.ban.vehicle_management.shared.enumeration.operations.ShiftAssignmentStatus.ACTIVE
              AND assignment.shift.status = com.ban.vehicle_management.shared.enumeration.operations.ShiftStatus.OPEN
            """)
    List<UUID> findOpenShiftParkingLotIdsByAccountId(@Param("accountId") UUID accountId);

    interface RecentEmployeeShiftProjection {
        UUID getAssignmentId();

        UUID getShiftId();

        LocalDate getShiftDate();

        ShiftType getShiftType();

        Instant getStartTime();

        Instant getEndTime();

        String getLocationName();

        ShiftAssignmentStatus getStatus();
    }

    List<ShiftAssignmentEntity> findAllByShiftIdAndStatusOrderByCreatedAtAsc(
            UUID shiftId,
            ShiftAssignmentStatus status
    );

    List<ShiftAssignmentEntity>
    findAllByEmployeeIdAndStatusAndShift_ShiftDateBetweenOrderByShift_StartTimeAsc(
            UUID employeeId,
            ShiftAssignmentStatus status,
            LocalDate fromDate,
            LocalDate toDate
    );

    long countByShiftIdAndStatus(
            UUID shiftId,
            ShiftAssignmentStatus status
    );

    boolean existsByShiftIdAndEmployeeIdAndStatus(
            UUID shiftId,
            UUID employeeId,
            ShiftAssignmentStatus status
    );

    boolean existsByShiftIdAndEmployeeIdAndStatusAndShiftAssignmentIdNot(
            UUID shiftId,
            UUID employeeId,
            ShiftAssignmentStatus status,
            UUID shiftAssignmentId
    );

    boolean existsByShiftIdAndGateIdAndStatus(
            UUID shiftId,
            UUID gateId,
            ShiftAssignmentStatus status
    );

    boolean existsByShiftIdAndGateIdAndStatusAndShiftAssignmentIdNot(
            UUID shiftId,
            UUID gateId,
            ShiftAssignmentStatus status,
            UUID shiftAssignmentId
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT assignment
            FROM ShiftAssignmentEntity assignment
            WHERE assignment.shiftAssignmentId = :assignmentId
            """)
    Optional<ShiftAssignmentEntity> findByIdForUpdate(
            @Param("assignmentId") UUID assignmentId
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT assignment
            FROM ShiftAssignmentEntity assignment
            WHERE assignment.shiftAssignmentId IN :assignmentIds
            """)
    List<ShiftAssignmentEntity> findAllByIdsForUpdate(
            @Param("assignmentIds") Collection<UUID> assignmentIds
    );

    List<ShiftAssignmentEntity>
    findAllByShiftIdInAndStatusOrderByCreatedAtAsc(
            Collection<UUID> shiftIds,
            ShiftAssignmentStatus status
    );

    List<ShiftAssignmentEntity> findAllByShiftIdAndStatusNotOrderByCreatedAtAsc(
            UUID shiftId,
            ShiftAssignmentStatus status
    );

    List<ShiftAssignmentEntity>
    findAllByEmployeeIdAndStatusNotAndShift_ShiftDateBetweenOrderByShift_StartTimeAsc(
            UUID employeeId,
            ShiftAssignmentStatus status,
            LocalDate fromDate,
            LocalDate toDate
    );

    List<ShiftAssignmentEntity>
    findAllByShiftIdInAndStatusNotOrderByCreatedAtAsc(
            Collection<UUID> shiftIds,
            ShiftAssignmentStatus status
    );

    long countByShiftIdAndStatusNot(
            UUID shiftId,
            ShiftAssignmentStatus status
    );

    boolean existsByShiftIdAndEmployeeIdAndStatusNot(
            UUID shiftId,
            UUID employeeId,
            ShiftAssignmentStatus status
    );

    boolean existsByShiftIdAndEmployeeIdAndStatusNotAndShiftAssignmentIdNot(
            UUID shiftId,
            UUID employeeId,
            ShiftAssignmentStatus status,
            UUID shiftAssignmentId
    );

    boolean existsByShiftIdAndGateIdAndStatusNot(
            UUID shiftId,
            UUID gateId,
            ShiftAssignmentStatus status
    );

    boolean existsByShiftIdAndGateIdAndStatusNotAndShiftAssignmentIdNot(
            UUID shiftId,
            UUID gateId,
            ShiftAssignmentStatus status,
            UUID shiftAssignmentId
    );

    @Query("""
            SELECT assignment.shiftAssignmentId AS assignmentId,
                   shift.shiftId AS shiftId,
                   shift.shiftDate AS shiftDate,
                   shift.shiftType AS shiftType,
                   shift.startTime AS startTime,
                   shift.endTime AS endTime,
                   COALESCE(gate.name, gate.code) AS locationName,
                   assignment.status AS status
            FROM ShiftAssignmentEntity assignment
                     JOIN assignment.shift shift
                     LEFT JOIN assignment.gate gate
            WHERE assignment.employeeId = :employeeId
              AND assignment.status <> :removedStatus
            ORDER BY shift.shiftDate DESC, shift.startTime DESC
            """)
    List<RecentEmployeeShiftProjection> findRecentEmployeeShifts(
            @Param("employeeId") UUID employeeId,
            @Param("removedStatus") ShiftAssignmentStatus removedStatus,
            Pageable pageable
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
        update operations.shift_assignments assignment
           set status = 'REMOVED', updated_at = :now
          from operations.shifts shift
          join parking.parking_lots lot on lot.parking_lot_id = shift.parking_lot_id
         where assignment.shift_id = shift.shift_id
           and lot.organization_id in (:organizationIds) and assignment.status <> 'REMOVED'
        """, nativeQuery = true)
    int removeAllByOrganizationIds(
            @Param("organizationIds") Collection<UUID> organizationIds,
            @Param("now") Instant now);
}
