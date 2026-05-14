package com.ban.vehicle_management.infrastructure.persistence.operations.shift;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "shift_assignments", schema = "operations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ShiftAssignmentEntity {

    @Id
    @Column(name = "shift_assignment_id", nullable = false)
    private UUID shiftAssignmentId;

    @Column(name = "shift_id", nullable = false)
    private UUID shiftId;

    @Column(name = "employee_id", nullable = false)
    private UUID employeeId;

    @Column(name = "role_in_shift", nullable = false)
    private String roleInShift;

    @Column(name = "assigned_at", nullable = false)
    private Instant assignedAt;

}
