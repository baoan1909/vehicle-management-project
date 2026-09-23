package com.ban.vehicle_management.application.accesscontrol.subscription.model.result;

import java.math.BigDecimal;

public record SubscriptionVoucherQuote(
        String voucherCode,
        BigDecimal baseAmount,
        BigDecimal discountAmount,
        BigDecimal finalAmount
) {
}
