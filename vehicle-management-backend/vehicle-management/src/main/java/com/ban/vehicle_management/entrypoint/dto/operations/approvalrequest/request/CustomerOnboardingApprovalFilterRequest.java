package com.ban.vehicle_management.entrypoint.dto.operations.approvalrequest.request;

import com.ban.vehicle_management.shared.enumeration.operations.ApprovalRequestStatus;

public record CustomerOnboardingApprovalFilterRequest(
        String keyword,
        ApprovalRequestStatus status
) {
}
