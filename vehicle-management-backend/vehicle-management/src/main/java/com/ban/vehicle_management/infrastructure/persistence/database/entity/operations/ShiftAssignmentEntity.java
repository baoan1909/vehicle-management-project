package com.ban.vehicle_management.infrastructure.persistence.database.entity.operations;

import com.ban.vehicle_management.infrastructure.persistence.database.entity.common.AuditableEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.parking.GateEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.people.EmployeeEntity;
import com.ban.vehicle_management.shared.enumeration.operations.ShiftAssignmentStatus;
import jakarta.persistence.*;
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
public class ShiftAssignmentEntity extends AuditableEntity {

    @Id
    @Column(name = "shift_assignment_id", nullable = false)
    private UUID shiftAssignmentId;

    @Column(name = "shift_id", nullable = false)
    private UUID shiftId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "shift_id",
            referencedColumnName = "shift_id",
            insertable = false,
            updatable = false
    )
    private ShiftEntity shift;

    @Column(name = "employee_id", nullable = false)
    private UUID employeeId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "employee_id",
            referencedColumnName = "employee_id",
            insertable = false,
            updatable = false
    )
    private EmployeeEntity employee;

    @Column(name = "gate_id")
    private UUID gateId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "gate_id",
            referencedColumnName = "gate_id",
            insertable = false,
            updatable = false
    )
    private GateEntity gate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ShiftAssignmentStatus status;
}