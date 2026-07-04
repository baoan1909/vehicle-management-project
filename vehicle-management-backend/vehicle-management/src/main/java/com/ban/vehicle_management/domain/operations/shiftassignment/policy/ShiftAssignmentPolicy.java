package com.ban.vehicle_management.domain.operations.shiftassignment.policy;

import com.ban.vehicle_management.domain.operations.shiftassignment.model.ShiftAssignment;
import com.ban.vehicle_management.shared.enumeration.operations.ShiftAssignmentStatus;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import com.ban.vehicle_management.shared.exception.ConflictException;

public class ShiftAssignmentPolicy {

    public void initializeNew(ShiftAssignment assignment) {
        requireAssignment(assignment);
        assignment.setStatus(ShiftAssignmentStatus.DRAFT);
        validateState(assignment);
    }

    public void schedule(ShiftAssignment assignment) {
        requireStatus(assignment, ShiftAssignmentStatus.DRAFT);
        assignment.setStatus(ShiftAssignmentStatus.SCHEDULED);
        validateState(assignment);
    }

    public void activate(ShiftAssignment assignment) {
        requireStatus(assignment, ShiftAssignmentStatus.SCHEDULED);
        assignment.setStatus(ShiftAssignmentStatus.ACTIVE);
        validateState(assignment);
    }

    public void validateState(ShiftAssignment assignment) {
        requireAssignment(assignment);

        requireField(
                assignment.getShiftAssignmentId(),
                "shiftAssignmentId"
        );
        requireField(assignment.getShiftId(), "shiftId");
        requireField(assignment.getEmployeeId(), "employeeId");
        requireField(assignment.getGateId(), "gateId");
        requireField(assignment.getStatus(), "status");
    }

    private void requireStatus(
            ShiftAssignment assignment,
            ShiftAssignmentStatus expectedStatus
    ) {
        requireAssignment(assignment);

        if (assignment.getStatus() != expectedStatus) {
            throw new ConflictException(
                    "Shift assignment must be in " + expectedStatus + " status"
            );
        }
    }

    public void remove(ShiftAssignment assignment) {
        requireAssignment(assignment);

        if (assignment.getStatus()
                == ShiftAssignmentStatus.REMOVED) {
            return;
        }

        assignment.setStatus(ShiftAssignmentStatus.REMOVED);
        validateState(assignment);
    }

    private void requireAssignment(ShiftAssignment assignment) {
        requireField(assignment, "shiftAssignment");
    }

    private void requireField(Object value, String fieldName) {
        if (value == null) {
            throw new BadRequestException(
                    fieldName + " must not be null"
            );
        }
    }
}