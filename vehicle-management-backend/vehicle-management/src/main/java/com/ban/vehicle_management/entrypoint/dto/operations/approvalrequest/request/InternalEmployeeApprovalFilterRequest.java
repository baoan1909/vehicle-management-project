package com.ban.vehicle_management.entrypoint.dto.operations.approvalrequest.request;

import com.ban.vehicle_management.shared.enumeration.iam.AdminProvisionableAccountRoleCode;
import com.ban.vehicle_management.shared.enumeration.operations.ApprovalRequestStatus;

public record InternalEmployeeApprovalFilterRequest(
        String keyword,
        AdminProvisionableAccountRoleCode roleCode,
        ApprovalRequestStatus status
) {
}
