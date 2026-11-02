package com.ban.vehicle_management.infrastructure.persistence.database.entity.people;

import com.ban.vehicle_management.infrastructure.persistence.database.entity.common.AuditableEntity;
import com.ban.vehicle_management.shared.enumeration.people.EmployeeParkingLotAssignmentSource;
import com.ban.vehicle_management.shared.enumeration.people.EmployeeParkingLotAssignmentStatus;
import com.ban.vehicle_management.shared.enumeration.people.EmployeeParkingLotAssignmentType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "employee_parking_lot_assignments", schema = "people")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeParkingLotAssignmentEntity extends AuditableEntity {
    @Id @Column(name = "assignment_id", nullable = false)
    private UUID assignmentId;
    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;
    @Column(name = "parking_lot_id", nullable = false)
    private UUID parkingLotId;
    @Column(name = "employee_id", nullable = false)
    private UUID employeeId;
    @Enumerated(EnumType.STRING) @Column(name = "assignment_type", nullable = false)
    private EmployeeParkingLotAssignmentType assignmentType;
    @Enumerated(EnumType.STRING) @Column(name = "status", nullable = false)
    private EmployeeParkingLotAssignmentStatus status;
    @Column(name = "effective_from", nullable = false)
    private Instant effectiveFrom;
    @Column(name = "effective_to")
    private Instant effectiveTo;
    @Column(name = "assigned_by_account_id", nullable = false)
    private UUID assignedByAccountId;
    @Enumerated(EnumType.STRING) @Column(name = "assignment_source", nullable = false)
    private EmployeeParkingLotAssignmentSource assignmentSource;
    @Column(name = "ended_by_account_id")
    private UUID endedByAccountId;
    @Column(name = "end_reason", length = 500)
    private String endReason;
    @Version @Column(name = "version", nullable = false)
    private Long version;
}
