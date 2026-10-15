package com.ban.vehicle_management.entrypoint.dto.billing.platformfee.response;

import com.ban.vehicle_management.shared.enumeration.billing.PlatformFeeRuleVersionStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PlatformFeeVersionAdminResponse(
        UUID platformFeeRuleVersionId,
        UUID platformFeeTypeId,
        int version,
        UUID previousVersionId,
        BigDecimal feeRate,
        Instant effectiveFrom,
        Instant effectiveTo,
        PlatformFeeRuleVersionStatus status,
        Instant createdAt,
        UUID createdBy,
        Instant updatedAt,
        UUID updatedBy,
        Instant activatedAt,
        UUID activatedBy,
        Instant deactivatedAt,
        UUID deactivatedBy,
        Instant archivedAt,
        UUID archivedBy
) {
}
