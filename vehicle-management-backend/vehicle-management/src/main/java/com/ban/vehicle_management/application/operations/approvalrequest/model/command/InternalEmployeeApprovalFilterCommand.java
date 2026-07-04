package com.ban.vehicle_management.application.operations.approvalrequest.model.command;

import com.ban.vehicle_management.shared.enumeration.iam.AdminProvisionableAccountRoleCode;
import com.ban.vehicle_management.shared.enumeration.operations.ApprovalRequestStatus;

public record InternalEmployeeApprovalFilterCommand(
        String keyword,
        AdminProvisionableAccountRoleCode roleCode,
        ApprovalRequestStatus status
) {
}
