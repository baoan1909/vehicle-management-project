package com.ban.vehicle_management.domain.iam.account.model;

import java.util.UUID;

public record AccountDeletionBlocker(
        String code,
        String message,
        String resourceType,
        UUID resourceId,
        UUID organizationId,
        UUID parkingLotId,
        String remediationAction,
        String remediationUrl
) {
    public static AccountDeletionBlocker of(String code, String message, String resourceType, UUID resourceId) {
        return new AccountDeletionBlocker(code, message, resourceType, resourceId,
                null, null, null, null);
    }
}
