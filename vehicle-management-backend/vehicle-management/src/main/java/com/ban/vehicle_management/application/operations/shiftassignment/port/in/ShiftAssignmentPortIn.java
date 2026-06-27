package com.ban.vehicle_management.application.operations.shiftassignment.port.in;

import com.ban.vehicle_management.domain.operations.shiftassignment.model.ShiftAssignment;
import com.ban.vehicle_management.shared.enumeration.operations.ShiftAssignmentStatus;
import com.ban.vehicle_management.shared.enumeration.operations.ShiftType;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface ShiftAssignmentPortIn {

    ShiftAssignment createAssignment(
            UUID shiftId,
            ShiftAssignment assignment
    );

    ShiftAssignment getAssignmentById(UUID assignmentId);

    List<ShiftAssignment> getAssignmentsByShift(
            UUID shiftId,
            ShiftAssignmentStatus status
    );

    List<ShiftAssignment> getAssignments(
            UUID parkingLotId,
            UUID shiftId,
            UUID employeeId,
            UUID gateId,
            ShiftAssignmentStatus status,
            LocalDate fromDate,
            LocalDate toDate,
            ShiftType shiftType
    );

    List<ShiftAssignment> getMyAssignments(
            LocalDate fromDate,
            LocalDate toDate,
            ShiftAssignmentStatus status
    );

    ShiftAssignment updateAssignment(
            UUID assignmentId,
            ShiftAssignment request
    );

    ShiftAssignment replaceAssignment(
            UUID assignmentId,
            UUID replacementEmployeeId,
            String reason
    );

    List<ShiftAssignment> swapAssignments(
            UUID firstAssignmentId,
            UUID secondAssignmentId,
            String reason
    );

    void deleteAssignment(UUID assignmentId);
}