package com.ban.vehicle_management.application.billing.platformfee.model.result;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PlatformFeeConfigurationResult(
        BigDecimal maxPartnerTotalRate,
        long version,
        Instant updatedAt,
        UUID updatedBy
) {
}
