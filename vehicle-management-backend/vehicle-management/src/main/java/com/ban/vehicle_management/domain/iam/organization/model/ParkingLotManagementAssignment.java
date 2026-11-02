package com.ban.vehicle_management.domain.iam.organization.model;

import com.ban.vehicle_management.domain.common.model.AuditableDomainModel;
import com.ban.vehicle_management.shared.enumeration.iam.ManagementAssignmentRole;
import com.ban.vehicle_management.shared.enumeration.iam.ManagementAssignmentStatus;
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
public class ParkingLotManagementAssignment extends AuditableDomainModel {
    private UUID assignmentId;
    private UUID organizationId;
    private UUID parkingLotId;
    private UUID managerAccountId;
    private ManagementAssignmentRole managementRole;
    private ManagementAssignmentStatus status;
    private Instant effectiveFrom;
    private Instant effectiveTo;
    private Instant acceptedAt;
    private UUID assignedBy;
    private UUID endedBy;
    private String endReason;
    private String handoverNote;
    private Long version;
}
