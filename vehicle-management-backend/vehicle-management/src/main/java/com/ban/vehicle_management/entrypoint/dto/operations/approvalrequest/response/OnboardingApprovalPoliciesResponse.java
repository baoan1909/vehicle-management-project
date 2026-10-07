package com.ban.vehicle_management.entrypoint.dto.operations.approvalrequest.response;

import java.time.Instant;

public record OnboardingApprovalPoliciesResponse(
        boolean customerAutoApproveEnabled,
        Instant customerEffectiveFrom,
        boolean partnerAutoApproveEnabled,
        Instant partnerEffectiveFrom,
        boolean avatarAutoApproveEnabled,
        Instant avatarEffectiveFrom
) {
}
