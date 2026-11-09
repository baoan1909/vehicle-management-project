package com.ban.vehicle_management.entrypoint.dto.billing.platformfee.request;

import java.math.BigDecimal;
import jakarta.validation.constraints.NotNull;

public record UpdatePlatformFeeConfigurationRequest(
        @NotNull BigDecimal maxPartnerTotalRate,
        @NotNull Long version
) {
}
