package com.ban.vehicle_management.application.iam.account.model.result;

import com.ban.vehicle_management.shared.enumeration.iam.AccountDeletionRequestStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AccountDeletionAuditResult(
        UUID requestId,
        UUID accountId,
        AccountDeletionRequestStatus status,
        Instant requestedAt,
        Instant scheduledDeletionAt,
        Instant cancelledAt,
        Instant completedAt,
        int attemptCount,
        String lastErrorCode,
        List<DeletionCleanupArchiveResult> cleanupItems
) {}
