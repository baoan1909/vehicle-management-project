package com.ban.vehicle_management.domain.operations.shift.model;

import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ShiftAssignment {

    private UUID shiftAssignmentId;
    private UUID shiftId;
    private UUID employeeId;
    private String roleInShift;
    private Instant assignedAt;
}

