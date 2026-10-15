package com.ban.vehicle_management.application.billing.platformfee.config;

import java.math.BigDecimal;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.billing.platform-fee")
public record PlatformFeeProperties(BigDecimal maxPartnerTotalRate) {

    public PlatformFeeProperties {
        if (maxPartnerTotalRate == null) {
            maxPartnerTotalRate = new BigDecimal("0.30");
        }
        if (maxPartnerTotalRate.compareTo(BigDecimal.ZERO) < 0
                || maxPartnerTotalRate.compareTo(BigDecimal.ONE) > 0) {
            throw new IllegalArgumentException("app.billing.platform-fee.max-partner-total-rate must be between 0 and 1");
        }
    }
}
