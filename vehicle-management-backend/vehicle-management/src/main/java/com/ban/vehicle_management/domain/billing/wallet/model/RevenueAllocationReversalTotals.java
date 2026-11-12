package com.ban.vehicle_management.domain.billing.wallet.model;

import java.math.BigDecimal;

public record RevenueAllocationReversalTotals(
        BigDecimal refundAmount,
        BigDecimal platformFeeAmount,
        BigDecimal voucherCommissionAmount,
        BigDecimal partnerPayableAmount,
        BigDecimal gatewayFeeAmount,
        BigDecimal discountAmount) {

    public static RevenueAllocationReversalTotals zero() {
        return new RevenueAllocationReversalTotals(
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
    }
}
