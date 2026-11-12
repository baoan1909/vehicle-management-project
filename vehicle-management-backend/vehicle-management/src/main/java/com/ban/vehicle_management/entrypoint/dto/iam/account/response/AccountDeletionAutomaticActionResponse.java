package com.ban.vehicle_management.entrypoint.dto.iam.account.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record AccountDeletionAutomaticActionResponse(
        String code,
        String message,
        String resourceType,
        UUID resourceId,
        String action,
        Instant scheduledAt,
        BigDecimal estimatedRefundAmount,
        String remediationUrl
) {
}
