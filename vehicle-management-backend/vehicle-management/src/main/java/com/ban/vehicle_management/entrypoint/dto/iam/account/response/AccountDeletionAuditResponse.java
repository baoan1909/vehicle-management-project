package com.ban.vehicle_management.entrypoint.dto.iam.account.response;

import com.ban.vehicle_management.shared.enumeration.iam.AccountDeletionRequestStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AccountDeletionAuditResponse(
        UUID requestId,
        UUID accountId,
        AccountDeletionRequestStatus status,
        Instant requestedAt,
        Instant scheduledDeletionAt,
        Instant cancelledAt,
        Instant completedAt,
        int attemptCount,
        String lastErrorCode,
        List<DeletionCleanupArchiveResponse> cleanupItems
) {}
