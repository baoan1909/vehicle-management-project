package com.ban.vehicle_management.entrypoint.dto.billing.platformfee.request;

import java.math.BigDecimal;
import java.time.Instant;

public record CreatePlatformFeeVersionRequest(
        BigDecimal feeRate,
        Instant effectiveFrom,
        Instant effectiveTo
) {
}
