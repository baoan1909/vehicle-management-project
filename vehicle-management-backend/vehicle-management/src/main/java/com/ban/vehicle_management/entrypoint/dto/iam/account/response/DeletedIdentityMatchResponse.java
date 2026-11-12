package com.ban.vehicle_management.entrypoint.dto.iam.account.response;

import com.ban.vehicle_management.shared.enumeration.iam.DeletedIdentityType;
import java.time.Instant;
import java.util.UUID;

public record DeletedIdentityMatchResponse(
        UUID accountId,
        UUID requestId,
        DeletedIdentityType identityType,
        Instant capturedAt
) {}
