package com.ban.vehicle_management.infrastructure.persistence.database.repository.operations;

import com.ban.vehicle_management.infrastructure.persistence.database.entity.operations.ShiftAssignmentEntity;
import com.ban.vehicle_management.shared.enumeration.operations.ShiftAssignmentStatus;
import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ShiftAssignmentRepository
        extends JpaRepository<ShiftAssignmentEntity, UUID>,
        JpaSpecificationExecutor<ShiftAssignmentEntity> {

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
}