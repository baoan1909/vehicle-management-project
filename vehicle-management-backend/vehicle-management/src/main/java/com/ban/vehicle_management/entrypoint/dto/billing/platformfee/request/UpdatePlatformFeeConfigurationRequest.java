package com.ban.vehicle_management.entrypoint.dto.billing.platformfee.request;

import java.math.BigDecimal;

public record UpdatePlatformFeeConfigurationRequest(
        BigDecimal maxPartnerTotalRate,
        Long version
) {
}
