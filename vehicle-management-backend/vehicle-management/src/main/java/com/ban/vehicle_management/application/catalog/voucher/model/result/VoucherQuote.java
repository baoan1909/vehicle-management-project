package com.ban.vehicle_management.application.catalog.voucher.model.result;

import java.math.BigDecimal;
import java.util.UUID;

public record VoucherQuote(
        UUID voucherId,
        String voucherCode,
        BigDecimal discountAmount,
        BigDecimal finalAmount
) {
    public static VoucherQuote withoutVoucher(BigDecimal amount) {
        return new VoucherQuote(null, null, BigDecimal.ZERO, amount);
    }
}
