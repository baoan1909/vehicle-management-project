package com.ban.vehicle_management.domain.billing.wallet.policy;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.ban.vehicle_management.domain.billing.wallet.model.Wallet;
import com.ban.vehicle_management.shared.enumeration.billing.WalletOwnerType;
import com.ban.vehicle_management.shared.enumeration.billing.WalletPurpose;
import com.ban.vehicle_management.shared.enumeration.billing.WalletStatus;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class WalletPolicyTest {

    private final WalletPolicy policy = new WalletPolicy();

    private Wallet wallet(WalletStatus status, BigDecimal available) {
        Wallet wallet = new Wallet();
        wallet.setWalletId(UUID.randomUUID());
        wallet.setOwnerType(WalletOwnerType.CUSTOMER);
        wallet.setCustomerId(UUID.randomUUID());
        wallet.setWalletPurpose(WalletPurpose.PERSONAL);
        wallet.setCurrency("VND");
        wallet.setAvailableBalance(available);
        wallet.setPendingBalance(BigDecimal.ZERO);
        wallet.setHeldBalance(BigDecimal.ZERO);
        wallet.setStatus(status);
        return wallet;
    }

    @Test
    void lockedWalletCannotDebit() {
        assertThrows(
                Exception.class,
                () -> policy.requireCanDebit(wallet(WalletStatus.LOCKED, BigDecimal.valueOf(100000))));
    }

    @Test
    void debitBlockedWalletStillAcceptsCredit() {
        assertDoesNotThrow(
                () -> policy.requireCanCredit(wallet(WalletStatus.DEBIT_BLOCKED, BigDecimal.ZERO)));
        assertThrows(
                Exception.class,
                () -> policy.requireCanDebit(wallet(WalletStatus.DEBIT_BLOCKED, BigDecimal.valueOf(50000))));
    }

    @Test
    void insufficientBalanceIsRejected() {
        assertThrows(
                Exception.class,
                () -> policy.requireSufficientAvailable(
                        wallet(WalletStatus.ACTIVE, BigDecimal.valueOf(10000)), BigDecimal.valueOf(20000)));
    }

    @Test
    void vndFractionalAmountIsRejected() {
        assertThrows(Exception.class, () -> policy.requireVndWholeUnit(new BigDecimal("1000.50")));
        assertDoesNotThrow(() -> policy.requireVndWholeUnit(new BigDecimal("1000")));
    }

    @Test
    void customerWalletWithOrganizationIsInvalid() {
        Wallet wallet = wallet(WalletStatus.ACTIVE, BigDecimal.ZERO);
        wallet.setOrganizationId(UUID.randomUUID());
        assertThrows(Exception.class, () -> policy.validateState(wallet));
    }
}
