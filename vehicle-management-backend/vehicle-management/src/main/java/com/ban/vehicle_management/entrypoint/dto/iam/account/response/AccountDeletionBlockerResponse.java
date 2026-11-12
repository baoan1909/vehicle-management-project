package com.ban.vehicle_management.entrypoint.dto.iam.account.response;

import java.util.UUID;

public record AccountDeletionBlockerResponse(
        String code,
        String message,
        String resourceType,
        UUID resourceId,
        UUID organizationId,
        UUID parkingLotId,
        String remediationAction,
        String remediationUrl
) {
}
