package com.ban.vehicle_management.infrastructure.persistence.database.entity.iam;

import com.ban.vehicle_management.infrastructure.persistence.database.entity.common.AuditableEntity;
import com.ban.vehicle_management.shared.enumeration.iam.ManagementAssignmentRole;
import com.ban.vehicle_management.shared.enumeration.iam.ManagementAssignmentStatus;
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
@Table(name = "parking_lot_management_assignments", schema = "iam")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ParkingLotManagementAssignmentEntity extends AuditableEntity {
    @Id @Column(name = "assignment_id", nullable = false)
    private UUID assignmentId;
    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;
    @Column(name = "parking_lot_id", nullable = false)
    private UUID parkingLotId;
    @Column(name = "manager_account_id", nullable = false)
    private UUID managerAccountId;
    @Enumerated(EnumType.STRING) @Column(name = "management_role", nullable = false)
    private ManagementAssignmentRole managementRole;
    @Enumerated(EnumType.STRING) @Column(name = "status", nullable = false)
    private ManagementAssignmentStatus status;
    @Column(name = "effective_from", nullable = false)
    private Instant effectiveFrom;
    @Column(name = "effective_to")
    private Instant effectiveTo;
    @Column(name = "accepted_at")
    private Instant acceptedAt;
    @Column(name = "assigned_by", nullable = false)
    private UUID assignedBy;
    @Column(name = "ended_by")
    private UUID endedBy;
    @Column(name = "end_reason", length = 500)
    private String endReason;
    @Column(name = "handover_note", length = 1000)
    private String handoverNote;
    @Version @Column(name = "version", nullable = false)
    private Long version;
}
