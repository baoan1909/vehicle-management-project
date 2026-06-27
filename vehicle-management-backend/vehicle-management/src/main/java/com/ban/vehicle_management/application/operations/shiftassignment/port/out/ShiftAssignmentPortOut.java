package com.ban.vehicle_management.application.operations.shiftassignment.port.out;

import com.ban.vehicle_management.domain.operations.shiftassignment.model.ShiftAssignment;
import com.ban.vehicle_management.shared.enumeration.operations.ShiftAssignmentStatus;
import com.ban.vehicle_management.shared.enumeration.operations.ShiftType;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ShiftAssignmentPortOut {

    ShiftAssignment save(ShiftAssignment assignment);

    List<ShiftAssignment> saveAll(
            List<ShiftAssignment> assignments
    );

    Optional<ShiftAssignment> findById(UUID assignmentId);

    Optional<ShiftAssignment> findByIdForUpdate(UUID assignmentId);

    List<ShiftAssignment> findAllByIdsForUpdate(
            Collection<UUID> assignmentIds
    );

    List<ShiftAssignment> findByShiftId(
            UUID shiftId,
            ShiftAssignmentStatus status
    );

    List<ShiftAssignment> findActiveEmployeeSchedule(
            UUID employeeId,
            LocalDate fromDate,
            LocalDate toDate
    );

    List<ShiftAssignment> findAll(
            UUID parkingLotId,
            UUID shiftId,
            UUID employeeId,
            UUID gateId,
            ShiftAssignmentStatus status,
            LocalDate fromDate,
            LocalDate toDate,
            ShiftType shiftType
    );

    long countActiveByShiftId(UUID shiftId);

    boolean existsActiveEmployeeInShift(
            UUID shiftId,
            UUID employeeId
    );

    boolean existsActiveEmployeeInShiftExcluding(
            UUID shiftId,
            UUID employeeId,
            UUID assignmentId
    );

    boolean existsActiveGateInShift(
            UUID shiftId,
            UUID gateId
    );

    boolean existsActiveGateInShiftExcluding(
            UUID shiftId,
            UUID gateId,
            UUID assignmentId
    );

    List<ShiftAssignment> findByShiftIds(
            Collection<UUID> shiftIds,
            ShiftAssignmentStatus status
    );

    List<ShiftAssignment> findNotRemovedByShiftId(UUID shiftId);

    List<ShiftAssignment> findNotRemovedEmployeeSchedule(
            UUID employeeId,
            LocalDate fromDate,
            LocalDate toDate
    );

    List<ShiftAssignment> findNotRemovedByShiftIds(
            Collection<UUID> shiftIds
    );

    long countNotRemovedByShiftId(UUID shiftId);

    boolean existsNotRemovedEmployeeInShift(
            UUID shiftId,
            UUID employeeId
    );

    boolean existsNotRemovedEmployeeInShiftExcluding(
            UUID shiftId,
            UUID employeeId,
            UUID assignmentId
    );

    boolean existsNotRemovedGateInShift(
            UUID shiftId,
            UUID gateId
    );

    boolean existsNotRemovedGateInShiftExcluding(
            UUID shiftId,
            UUID gateId,
            UUID assignmentId
    );
}