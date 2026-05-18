package com.ban.vehicle_management.domain.operations.shift.policy;

import com.ban.vehicle_management.domain.operations.shift.model.ShiftAssignment;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import com.ban.vehicle_management.shared.utils.TextValidationUtils;
import java.time.Instant;

public class ShiftAssignmentPolicy {

    public void initialize(ShiftAssignment shiftAssignment) {
        requireShiftAssignment(shiftAssignment);
        requireField(shiftAssignment.getShiftId(), "shiftId");
        requireField(shiftAssignment.getEmployeeId(), "employeeId");
        shiftAssignment.setRoleInShift(TextValidationUtils.normalizeCode(shiftAssignment.getRoleInShift(), "roleInShift", 50));
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

}

