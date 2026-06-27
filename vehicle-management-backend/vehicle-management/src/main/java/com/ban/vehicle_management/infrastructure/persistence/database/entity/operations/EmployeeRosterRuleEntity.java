package com.ban.vehicle_management.infrastructure.persistence.database.entity.operations;

import com.ban.vehicle_management.infrastructure.persistence.database.entity.common.AuditableEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.parking.GateEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.parking.ParkingLotEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.people.EmployeeEntity;
import com.ban.vehicle_management.shared.enumeration.operations.AssignmentMode;
import com.ban.vehicle_management.shared.enumeration.operations.RosterRuleStatus;
import com.ban.vehicle_management.shared.enumeration.operations.ShiftType;
import jakarta.persistence.*;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "employee_roster_rules", schema = "operations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeRosterRuleEntity extends AuditableEntity {

    @Id
    @Column(name = "roster_rule_id", nullable = false)
    private UUID rosterRuleId;

    @Column(name = "parking_lot_id", nullable = false)
    private UUID parkingLotId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "parking_lot_id",
            referencedColumnName = "parking_lot_id",
            insertable = false,
            updatable = false
    )
    private ParkingLotEntity parkingLot;

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

    @Enumerated(EnumType.STRING)
    @Column(name = "preferred_shift_type")
    private ShiftType preferredShiftType;

    @Column(name = "preferred_gate_id")
    private UUID preferredGateId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "preferred_gate_id",
            referencedColumnName = "gate_id",
            insertable = false,
            updatable = false
    )
    private GateEntity preferredGate;

    @Enumerated(EnumType.STRING)
    @Column(name = "weekly_day_off", nullable = false)
    private DayOfWeek weeklyDayOff;

    @Enumerated(EnumType.STRING)
    @Column(name = "assignment_mode", nullable = false)
    private AssignmentMode assignmentMode;

    @Column(name = "effective_from", nullable = false)
    private LocalDate effectiveFrom;

    @Column(name = "effective_to")
    private LocalDate effectiveTo;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private RosterRuleStatus status;
}