package com.ban.vehicle_management.application.iam.account.model.result;

import com.ban.vehicle_management.shared.enumeration.iam.AccountDeletionRequestStatus;
import com.ban.vehicle_management.shared.enumeration.iam.AccountStatus;
import java.time.Instant;
import java.util.UUID;

public record DeletedAccountArchiveResult(
        UUID accountId,
        AccountStatus accountStatus,
        UUID requestId,
        AccountDeletionRequestStatus requestStatus,
        Instant requestedAt,
        Instant scheduledDeletionAt,
        Instant completedAt
) {}
