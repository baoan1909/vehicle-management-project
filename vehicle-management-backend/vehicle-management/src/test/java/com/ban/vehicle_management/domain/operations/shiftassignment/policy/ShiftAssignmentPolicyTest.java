package com.ban.vehicle_management.domain.operations.shiftassignment.policy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.ban.vehicle_management.domain.operations.shiftassignment.model.ShiftAssignment;
import com.ban.vehicle_management.shared.enumeration.operations.ShiftAssignmentStatus;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ShiftAssignmentPolicyTest {

    private final ShiftAssignmentPolicy policy =
            new ShiftAssignmentPolicy();

    @Test
    void shouldInitializeDraftShiftAssignment() {
        ShiftAssignment assignment = validAssignment();
        assignment.setStatus(null);

        policy.initializeNew(assignment);

        assertEquals(
                ShiftAssignmentStatus.DRAFT,
                assignment.getStatus()
        );
    }

    @Test
    void shouldRemoveActiveShiftAssignment() {
        ShiftAssignment assignment = validAssignment();

        policy.remove(assignment);

        assertEquals(
                ShiftAssignmentStatus.REMOVED,
                assignment.getStatus()
        );
    }

    @Test
    void shouldKeepRemovedAssignmentUnchanged() {
        ShiftAssignment assignment = validAssignment();
        assignment.setStatus(ShiftAssignmentStatus.REMOVED);

        policy.remove(assignment);

        assertEquals(
                ShiftAssignmentStatus.REMOVED,
                assignment.getStatus()
        );
    }

    @Test
    void shouldRejectMissingGateId() {
        ShiftAssignment assignment = validAssignment();
        assignment.setGateId(null);

        assertThrows(
                BadRequestException.class,
                () -> policy.initializeNew(assignment)
        );
    }

    @Test
    void shouldRejectMissingAssignmentId() {
        ShiftAssignment assignment = validAssignment();
        assignment.setShiftAssignmentId(null);

        assertThrows(
                BadRequestException.class,
                () -> policy.initializeNew(assignment)
        );
    }

    private ShiftAssignment validAssignment() {
        ShiftAssignment assignment = new ShiftAssignment();
        assignment.setShiftAssignmentId(UUID.randomUUID());
        assignment.setShiftId(UUID.randomUUID());
        assignment.setEmployeeId(UUID.randomUUID());
        assignment.setGateId(UUID.randomUUID());
        assignment.setStatus(ShiftAssignmentStatus.ACTIVE);
        return assignment;
    }
}
