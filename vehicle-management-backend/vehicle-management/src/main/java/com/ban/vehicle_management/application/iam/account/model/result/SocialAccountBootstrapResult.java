package com.ban.vehicle_management.application.iam.account.model.result;

import java.util.UUID;

public record SocialAccountBootstrapResult(
        UUID accountId,
        String accountStatus,
        String roleCode,
        String provider,
        boolean created
) {
}
