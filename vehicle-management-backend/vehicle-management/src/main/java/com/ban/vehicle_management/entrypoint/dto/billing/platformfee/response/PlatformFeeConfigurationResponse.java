package com.ban.vehicle_management.entrypoint.dto.billing.platformfee.response;

import java.math.BigDecimal;

public record PlatformFeeConfigurationResponse(
        BigDecimal maxPartnerTotalRate
) {
}
