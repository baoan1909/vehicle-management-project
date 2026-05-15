package com.ban.vehicle_management.infrastructure.persistence.operations.shift;

import com.ban.vehicle_management.infrastructure.persistence.people.employee.EmployeeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "shift_id", referencedColumnName = "shift_id", insertable = false, updatable = false)
    private ShiftEntity shift;

    @Column(name = "employee_id", nullable = false)
    private UUID employeeId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", referencedColumnName = "employee_id", insertable = false, updatable = false)
    private EmployeeEntity employee;

    @Column(name = "role_in_shift", nullable = false)
    private String roleInShift;

    @Column(name = "assigned_at", nullable = false)
    private Instant assignedAt;

}
