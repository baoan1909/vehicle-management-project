package com.ban.vehicle_management.entrypoint.dto.iam.account.response;

import com.ban.vehicle_management.shared.enumeration.iam.AccountDeletionRequestStatus;
import java.time.Instant;
import java.util.UUID;

public record AccountDeletionResponse(
        UUID requestId,
        AccountDeletionRequestStatus status,
        Instant requestedAt,
        Instant scheduledDeletionAt,
        Instant cancelledAt,
        Instant completedAt,
        boolean cancellable
) {
}
