package com.ban.vehicle_management.domain.billing.wallet.policy;

import com.ban.vehicle_management.shared.exception.BadRequestException;
import java.math.BigDecimal;

/**
 * Validation for wallet top-up amounts (VND whole units, min/max).
 */
public class WalletTopupPolicy {

    public static final BigDecimal MIN_TOPUP = new BigDecimal("10000");
    public static final BigDecimal MAX_TOPUP = new BigDecimal("50000000");

    public void requireValidAmount(BigDecimal amount) {
        if (amount == null) {
            throw new BadRequestException("Top-up amount must not be null");
        }
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Top-up amount must be greater than zero");
        }
        if (amount.stripTrailingZeros().scale() > 0) {
            throw new BadRequestException("VND top-up amount must be a whole unit");
        }
        if (amount.compareTo(MIN_TOPUP) < 0) {
            throw new BadRequestException("Top-up amount must be at least " + MIN_TOPUP.toPlainString() + " VND");
        }
        if (amount.compareTo(MAX_TOPUP) > 0) {
            throw new BadRequestException("Top-up amount must not exceed " + MAX_TOPUP.toPlainString() + " VND");
        }
    }
}
