package com.ban.vehicle_management.entrypoint.dto.iam.account.response;

import java.util.UUID;

public record RegisterAccountResponse(
        UUID accountId,
        String accountStatus,
        String nextAction,
        boolean onboardingRequired
) {
}
