package com.ban.vehicle_management.domain.operations.shift.policy;

import com.ban.vehicle_management.domain.operations.shift.model.ShiftAssignment;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import java.time.Instant;

public class ShiftAssignmentPolicy {

    public void initialize(ShiftAssignment shiftAssignment) {
        requireShiftAssignment(shiftAssignment);
        requireField(shiftAssignment.getShiftId(), "shiftId");
        requireField(shiftAssignment.getEmployeeId(), "employeeId");
        shiftAssignment.setRoleInShift(normalizeRequired(shiftAssignment.getRoleInShift(), "roleInShift"));
        if (shiftAssignment.getAssignedAt() == null) {
            shiftAssignment.setAssignedAt(Instant.now());
        }
    }

    private void requireShiftAssignment(ShiftAssignment shiftAssignment) {
        requireField(shiftAssignment, "shiftAssignment");
    }

    private void requireField(Object value, String fieldName) {
        if (value == null) {
            throw new BadRequestException(fieldName + " must not be null");
        }
    }

    private String normalizeRequired(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new BadRequestException(fieldName + " must not be blank");
        }
        return value.trim();
    }
}

