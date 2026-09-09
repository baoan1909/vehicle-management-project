package com.ban.vehicle_management.application.operations.approvalrequest.model.command;

import com.ban.vehicle_management.shared.enumeration.operations.ApprovalRequestStatus;

public record InternalEmployeeApprovalFilterCommand(
        String keyword,
        String roleCode,
        ApprovalRequestStatus status
) {
}
