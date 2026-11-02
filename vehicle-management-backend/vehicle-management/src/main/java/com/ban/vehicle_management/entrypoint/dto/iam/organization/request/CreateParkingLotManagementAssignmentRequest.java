package com.ban.vehicle_management.entrypoint.dto.iam.organization.request;

import com.ban.vehicle_management.shared.enumeration.iam.ManagementAssignmentRole;
import java.time.Instant;
import java.util.UUID;

public record CreateParkingLotManagementAssignmentRequest(
        UUID managerAccountId,
        ManagementAssignmentRole managementRole,
        Instant effectiveAt,
        boolean acceptanceRequired,
        String handoverNote
) {}
