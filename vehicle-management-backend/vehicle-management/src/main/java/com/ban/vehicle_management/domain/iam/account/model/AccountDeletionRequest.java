package com.ban.vehicle_management.domain.iam.account.model;

import com.ban.vehicle_management.shared.enumeration.iam.AccountDeletionRequestStatus;
import java.time.Instant;
import java.util.UUID;

public record AccountDeletionRequest(
        UUID requestId,
        UUID accountId,
        AccountDeletionRequestStatus status,
        Instant requestedAt,
        Instant scheduledDeletionAt,
        Instant cancelledAt,
        Instant completedAt,
        int attemptCount,
        String lastErrorCode
) {
    public boolean cancellableAt(Instant now) {
        return status == AccountDeletionRequestStatus.GRACE_PERIOD
                && now != null
                && now.isBefore(scheduledDeletionAt);
    }
}
