package com.ban.vehicle_management.entrypoint.dto.iam.account.response;

import java.util.UUID;

public record SocialAccountBootstrapResponse(
        UUID accountId,
        String accountStatus,
        String roleCode,
        String provider,
        boolean created
) {
}
