package com.ban.vehicle_management.application.iam.account.model.result;

import java.util.UUID;

public record RegisterAccountResult(
        UUID accountId,
        String accountStatus,
        String nextAction,
        boolean onboardingRequired
) {
}
