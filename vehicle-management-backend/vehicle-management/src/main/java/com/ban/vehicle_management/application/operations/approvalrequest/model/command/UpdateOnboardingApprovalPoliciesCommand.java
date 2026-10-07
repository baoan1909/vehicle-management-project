package com.ban.vehicle_management.application.operations.approvalrequest.model.command;

public record UpdateOnboardingApprovalPoliciesCommand(
        boolean customerAutoApproveEnabled,
        boolean partnerAutoApproveEnabled,
        boolean avatarAutoApproveEnabled
) {
}
