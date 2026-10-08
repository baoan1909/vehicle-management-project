package com.ban.vehicle_management.domain.billing.wallet.policy;

import com.ban.vehicle_management.domain.billing.wallet.model.Wallet;
import com.ban.vehicle_management.shared.enumeration.billing.WalletStatus;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import java.math.BigDecimal;

/**
 * Pure domain rules for wallet state transitions.
 * No permission checks here; guards own authorization.
 */
public class WalletPolicy {

    public void requireActiveForProvision(Wallet wallet) {
        if (wallet == null) {
            return;
        }
        if (WalletStatus.CLOSED.equals(wallet.getStatus())) {
            throw new BadRequestException("Wallet is closed and cannot be re-provisioned");
        }
    }

    /** LOCKED wallets cannot be debited; DEBIT_BLOCKED still accepts credits (refunds). */
    public void requireCanDebit(Wallet wallet) {
        require(wallet, "wallet");
        if (WalletStatus.LOCKED.equals(wallet.getStatus()) || WalletStatus.CLOSED.equals(wallet.getStatus())) {
            throw new BadRequestException("Wallet is locked and cannot be debited");
        }
        if (WalletStatus.DEBIT_BLOCKED.equals(wallet.getStatus())) {
            throw new BadRequestException("Wallet debit is blocked");
        }
    }

    public void requireCanCredit(Wallet wallet) {
        require(wallet, "wallet");
        if (WalletStatus.LOCKED.equals(wallet.getStatus()) || WalletStatus.CLOSED.equals(wallet.getStatus())) {
            throw new BadRequestException("Wallet is locked and cannot be credited");
        }
        // DEBIT_BLOCKED wallets may still receive refunds/credits.
    }

    public void requireSufficientAvailable(Wallet wallet, BigDecimal amount) {
        require(wallet, "wallet");
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Amount must be greater than zero");
        }
        if (wallet.getAvailableBalance() == null || wallet.getAvailableBalance().compareTo(amount) < 0) {
            throw new BadRequestException("Insufficient wallet balance");
        }
    }

    public void requireVndWholeUnit(BigDecimal amount) {
        if (amount == null) {
            throw new BadRequestException("Amount must not be null");
        }
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Amount must be greater than zero");
        }
        try {
            if (amount.stripTrailingZeros().scale() > 0) {
                throw new BadRequestException("VND amount must be a whole unit");
            }
        } catch (ArithmeticException exception) {
            throw new BadRequestException("Invalid amount");
        }
    }

    public void validateState(Wallet wallet) {
        require(wallet, "wallet");
        if (wallet.getWalletId() == null) {
            throw new BadRequestException("walletId must not be null");
        }
        if (wallet.getOwnerType() == null) {
            throw new BadRequestException("ownerType must not be null");
        }
        if (wallet.getStatus() == null) {
            throw new BadRequestException("status must not be null");
        }
        assertNonNegative(wallet.getAvailableBalance(), "availableBalance");
        assertNonNegative(wallet.getPendingBalance(), "pendingBalance");
        assertNonNegative(wallet.getHeldBalance(), "heldBalance");
        switch (wallet.getOwnerType()) {
            case CUSTOMER -> {
                if (wallet.getCustomerId() == null || wallet.getOrganizationId() != null) {
                    throw new BadRequestException("Customer wallet must have customerId only");
                }
            }
            case ORGANIZATION -> {
                if (wallet.getOrganizationId() == null || wallet.getCustomerId() != null) {
                    throw new BadRequestException("Organization wallet must have organizationId only");
                }
            }
            case PLATFORM -> {
                if (wallet.getCustomerId() != null || wallet.getOrganizationId() != null) {
                    throw new BadRequestException("Platform account must not carry customer or organization");
                }
            }
            default -> throw new BadRequestException("Unsupported owner type");
        }
    }

    private void assertNonNegative(BigDecimal value, String field) {
        if (value == null || value.compareTo(BigDecimal.ZERO) < 0) {
            throw new BadRequestException(field + " must not be negative");
        }
    }

    private void require(Object value, String field) {
        if (value == null) {
            throw new BadRequestException(field + " must not be null");
        }
    }
}
