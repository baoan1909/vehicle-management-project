package com.ban.vehicle_management.application.iam.account.model.result;

import com.ban.vehicle_management.shared.enumeration.iam.AccountDeletionCleanupResourceType;
import com.ban.vehicle_management.shared.enumeration.iam.AccountDeletionCleanupStatus;
import java.time.Instant;
import java.util.UUID;

public record DeletionCleanupArchiveResult(
        UUID cleanupItemId,
        UUID organizationId,
        AccountDeletionCleanupResourceType resourceType,
        AccountDeletionCleanupStatus status,
        Instant scheduledAt,
        int attemptCount,
        Instant nextRetryAt,
        Instant completedAt
) {}
