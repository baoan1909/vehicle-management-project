package com.ban.vehicle_management.domain.billing.platformfee.policy;

import java.math.BigDecimal;

public final class PlatformFeeLimits {

    public static final BigDecimal MAX_PARTNER_TOTAL_RATE = new BigDecimal("0.30");

    private PlatformFeeLimits() {
    }
}
