package com.ban.vehicle_management.application.iam.account.model.result;

import java.util.UUID;

public record CompleteAccountOnboardingResult(
        UUID accountId,
        UUID userProfileId,
        UUID customerId,
        String accountStatus,
        boolean onboardingRequired
) {
}
