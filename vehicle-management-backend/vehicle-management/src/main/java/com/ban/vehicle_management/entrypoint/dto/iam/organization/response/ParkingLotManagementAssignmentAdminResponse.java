package com.ban.vehicle_management.entrypoint.dto.iam.organization.response;

import com.ban.vehicle_management.shared.enumeration.iam.ManagementAssignmentRole;
import com.ban.vehicle_management.shared.enumeration.iam.ManagementAssignmentStatus;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ParkingLotManagementAssignmentAdminResponse {
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
