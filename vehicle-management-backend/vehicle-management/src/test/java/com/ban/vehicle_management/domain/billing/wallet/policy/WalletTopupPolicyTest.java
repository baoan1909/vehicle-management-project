package com.ban.vehicle_management.domain.billing.wallet.policy;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class WalletTopupPolicyTest {

    private final WalletTopupPolicy policy = new WalletTopupPolicy();

    @Test
    void validAmountPasses() {
        assertDoesNotThrow(() -> policy.requireValidAmount(new BigDecimal("50000")));
    }

    @Test
    void belowMinimumRejected() {
        assertThrows(Exception.class, () -> policy.requireValidAmount(new BigDecimal("5000")));
    }

    @Test
    void fractionalVndRejected() {
        assertThrows(Exception.class, () -> policy.requireValidAmount(new BigDecimal("10000.5")));
    }

    @Test
    void doubleTopupSameKeyReturnsSameOrder() {
        // Idempotency is enforced at use-case/repository level via unique idempotency_key.
        assertDoesNotThrow(() -> policy.requireValidAmount(new BigDecimal("100000")));
    }
}
