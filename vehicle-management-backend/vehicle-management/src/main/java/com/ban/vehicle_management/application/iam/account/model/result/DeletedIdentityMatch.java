package com.ban.vehicle_management.application.iam.account.model.result;

import com.ban.vehicle_management.shared.enumeration.iam.DeletedIdentityType;
import java.time.Instant;
import java.util.UUID;

public record DeletedIdentityMatch(
        UUID accountId,
        UUID requestId,
        DeletedIdentityType identityType,
        Instant capturedAt
) {}
