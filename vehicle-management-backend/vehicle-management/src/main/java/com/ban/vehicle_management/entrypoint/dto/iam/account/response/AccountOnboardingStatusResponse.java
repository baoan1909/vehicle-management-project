package com.ban.vehicle_management.entrypoint.dto.iam.account.response;

import java.util.UUID;

public record AccountOnboardingStatusResponse(
        UUID accountId,
        String accountStatus,
        boolean onboardingRequired,
        UUID userProfileId,
        UUID customerId
) {
}
