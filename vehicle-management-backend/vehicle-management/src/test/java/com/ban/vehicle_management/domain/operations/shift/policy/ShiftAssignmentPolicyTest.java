package com.ban.vehicle_management.domain.operations.shift.policy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.ban.vehicle_management.domain.operations.shift.model.ShiftAssignment;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ShiftAssignmentPolicyTest {

    private final ShiftAssignmentPolicy shiftAssignmentPolicy = new ShiftAssignmentPolicy();

    @Test
    void shouldInitializeShiftAssignment() {
        ShiftAssignment shiftAssignment = new ShiftAssignment();
        shiftAssignment.setShiftId(UUID.randomUUID());
        shiftAssignment.setEmployeeId(UUID.randomUUID());
        shiftAssignment.setRoleInShift(" OPERATOR ");

        shiftAssignmentPolicy.initialize(shiftAssignment);

        assertEquals("OPERATOR", shiftAssignment.getRoleInShift());
        assertNotNull(shiftAssignment.getAssignedAt());
    }

    @Test
    void shouldRejectBlankRoleInShift() {
        ShiftAssignment shiftAssignment = new ShiftAssignment();
        shiftAssignment.setShiftId(UUID.randomUUID());
        shiftAssignment.setEmployeeId(UUID.randomUUID());
        shiftAssignment.setRoleInShift(" ");

        assertThrows(BadRequestException.class, () -> shiftAssignmentPolicy.initialize(shiftAssignment));
    }
}

