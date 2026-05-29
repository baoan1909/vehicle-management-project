package com.ban.vehicle_management.entrypoint.dto.iam.account.response;

import java.util.UUID;

public record CompleteAccountOnboardingResponse(
        UUID accountId,
        UUID userProfileId,
        UUID customerId,
        String accountStatus,
        boolean onboardingRequired
) {
}
