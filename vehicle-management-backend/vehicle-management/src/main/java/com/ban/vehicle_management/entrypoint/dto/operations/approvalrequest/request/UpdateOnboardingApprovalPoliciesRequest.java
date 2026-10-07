package com.ban.vehicle_management.entrypoint.dto.operations.approvalrequest.request;

public record UpdateOnboardingApprovalPoliciesRequest(
        boolean customerAutoApproveEnabled,
        boolean partnerAutoApproveEnabled,
        boolean avatarAutoApproveEnabled
) {
}
