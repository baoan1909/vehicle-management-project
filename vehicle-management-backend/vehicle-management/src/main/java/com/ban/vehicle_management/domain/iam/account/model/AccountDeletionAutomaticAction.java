package com.ban.vehicle_management.domain.iam.account.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record AccountDeletionAutomaticAction(
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
