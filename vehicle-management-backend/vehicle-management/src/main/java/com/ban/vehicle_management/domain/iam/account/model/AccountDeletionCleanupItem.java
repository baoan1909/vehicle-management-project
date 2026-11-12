package com.ban.vehicle_management.domain.iam.account.model;

import com.ban.vehicle_management.shared.enumeration.iam.AccountDeletionCleanupResourceType;
import java.util.UUID;

public record AccountDeletionCleanupItem(
        UUID cleanupItemId,
        UUID requestId,
        AccountDeletionCleanupResourceType resourceType,
        String externalIdentifier,
        int attemptCount) {
}
