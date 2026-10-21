package com.ban.vehicle_management.entrypoint.dto.billing.platformfee.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PlatformFeeConfigurationResponse(
        BigDecimal maxPartnerTotalRate,
        long version,
        Instant updatedAt,
        UUID updatedBy
) {
}
