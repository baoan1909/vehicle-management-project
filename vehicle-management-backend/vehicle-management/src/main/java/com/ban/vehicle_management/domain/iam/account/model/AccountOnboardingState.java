package com.ban.vehicle_management.domain.iam.account.model;

import com.ban.vehicle_management.shared.enumeration.iam.AccountStatus;

import java.util.UUID;

public record AccountOnboardingState(
        UUID accountId,
        UUID userProfileId,
        UUID customerId,
        AccountStatus accountStatus
) {
}
