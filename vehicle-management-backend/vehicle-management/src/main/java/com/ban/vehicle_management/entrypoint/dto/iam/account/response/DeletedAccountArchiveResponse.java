package com.ban.vehicle_management.entrypoint.dto.iam.account.response;

import com.ban.vehicle_management.shared.enumeration.iam.AccountDeletionRequestStatus;
import com.ban.vehicle_management.shared.enumeration.iam.AccountStatus;
import java.time.Instant;
import java.util.UUID;

public record DeletedAccountArchiveResponse(
        UUID accountId,
        AccountStatus accountStatus,
        UUID requestId,
        AccountDeletionRequestStatus requestStatus,
        Instant requestedAt,
        Instant scheduledDeletionAt,
        Instant completedAt
) {}
