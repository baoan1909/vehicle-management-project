package com.ban.vehicle_management.application.operations.approvalrequest.model.result;

import java.time.Instant;

public record OnboardingApprovalPoliciesResult(
        boolean customerAutoApproveEnabled,
        Instant customerEffectiveFrom,
        boolean partnerAutoApproveEnabled,
        Instant partnerEffectiveFrom,
        boolean avatarAutoApproveEnabled,
        Instant avatarEffectiveFrom
) {
}
