package com.ban.vehicle_management.application.iam.account.model.result;

import java.util.UUID;

public record AccountOnboardingStatusResult(
        UUID accountId,
        String accountStatus,
        boolean onboardingRequired,
        UUID userProfileId,
        UUID customerId
) {
}
