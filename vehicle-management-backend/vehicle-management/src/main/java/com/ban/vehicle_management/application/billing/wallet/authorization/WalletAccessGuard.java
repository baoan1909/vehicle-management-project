package com.ban.vehicle_management.application.billing.wallet.authorization;

import com.ban.vehicle_management.application.iam.account.port.in.CurrentAccountPortIn;
import com.ban.vehicle_management.application.iam.account.port.out.AccountProfilePortOut;
import com.ban.vehicle_management.application.iam.organization.port.out.OrganizationPortOut;
import com.ban.vehicle_management.domain.billing.wallet.model.Wallet;
import com.ban.vehicle_management.domain.iam.account.model.AccountProfileState;
import com.ban.vehicle_management.shared.enumeration.billing.WalletOwnerType;
import com.ban.vehicle_management.shared.enumeration.people.CustomerApprovalStatus;
import com.ban.vehicle_management.shared.enumeration.people.CustomerStatus;
import java.util.UUID;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

/**
 * Permission + scope guard for wallets.
 * Use cases check permission codes only; this guard enforces ownership/scope.
 * No role-code hardcoding here.
 */
@Component
public class WalletAccessGuard {

    public static final String READ_OWN = "WALLET_READ_OWN";
    public static final String TX_READ_OWN = "WALLET_TRANSACTION_READ_OWN";
    public static final String READ_PARTNER = "WALLET_READ_PARTNER";
    public static final String TX_READ_PARTNER = "WALLET_TRANSACTION_READ_PARTNER";
    public static final String READ_ALL = "WALLET_READ_ALL";
    public static final String TX_READ_ALL = "WALLET_TRANSACTION_READ_ALL";
    public static final String LOCK_ALL = "WALLET_LOCK_ALL";
    public static final String UNLOCK_ALL = "WALLET_UNLOCK_ALL";
    public static final String ADJUST_REQUEST_ALL = "WALLET_ADJUST_REQUEST_ALL";
    public static final String ADJUST_APPROVE_ALL = "WALLET_ADJUST_APPROVE_ALL";

    private final CurrentAccountPortIn currentAccountPortIn;
    private final AccountProfilePortOut accountProfilePortOut;
    private final OrganizationPortOut organizationPortOut;

    public WalletAccessGuard(
            CurrentAccountPortIn currentAccountPortIn,
            AccountProfilePortOut accountProfilePortOut,
            OrganizationPortOut organizationPortOut) {
        this.currentAccountPortIn = currentAccountPortIn;
        this.accountProfilePortOut = accountProfilePortOut;
        this.organizationPortOut = organizationPortOut;
    }

    public UUID resolveCurrentApprovedCustomerId() {
        UUID accountId = currentAccountPortIn.getCurrentAccountIdOrThrow();
        AccountProfileState state = accountProfilePortOut.findProfileStateByAccountId(accountId)
                .orElseThrow(() -> new AccessDeniedException("Access is denied"));
        if (state.customerId() == null
                || !CustomerStatus.ACTIVE.equals(state.customerStatus())
                || !CustomerApprovalStatus.APPROVED.equals(state.customerApprovalStatus())) {
            throw new AccessDeniedException("Access is denied");
        }
        return state.customerId();
    }

    public UUID resolveCurrentPartnerOrganizationId() {
        UUID accountId = currentAccountPortIn.getCurrentAccountIdOrThrow();
        var organizationIds = organizationPortOut.findActiveOrganizationIdsByAccountId(accountId);
        if (organizationIds == null || organizationIds.size() != 1) {
            throw new AccessDeniedException("Current account has no single active partner organization");
        }
        return organizationIds.iterator().next();
    }

    public void ensureCanReadCustomerWallet(Wallet wallet) {
        if (currentAccountPortIn.hasPermission(READ_ALL)) {
            return;
        }
        currentAccountPortIn.requirePermission(READ_OWN);
        UUID customerId = resolveCurrentApprovedCustomerId();
        if (!WalletOwnerType.CUSTOMER.equals(wallet.getOwnerType()) || !customerId.equals(wallet.getCustomerId())) {
            throw new AccessDeniedException("Access is denied");
        }
    }

    public void ensureCanReadPartnerWallet(Wallet wallet) {
        if (currentAccountPortIn.hasPermission(READ_ALL)) {
            return;
        }
        currentAccountPortIn.requirePermission(READ_PARTNER);
        UUID accountId = currentAccountPortIn.getCurrentAccountIdOrThrow();
        if (!WalletOwnerType.ORGANIZATION.equals(wallet.getOwnerType()) || wallet.getOrganizationId() == null) {
            throw new AccessDeniedException("Access is denied");
        }
        var memberships = organizationPortOut.findActiveOrganizationIdsByAccountId(accountId);
        if (!memberships.contains(wallet.getOrganizationId())) {
            throw new AccessDeniedException("Access is denied");
        }
    }

    public void ensureCanReadTransactions(Wallet wallet) {
        if (WalletOwnerType.CUSTOMER.equals(wallet.getOwnerType())) {
            if (currentAccountPortIn.hasPermission(TX_READ_ALL)) {
                return;
            }
            currentAccountPortIn.requirePermission(TX_READ_OWN);
            UUID customerId = resolveCurrentApprovedCustomerId();
            if (!customerId.equals(wallet.getCustomerId())) {
                throw new AccessDeniedException("Access is denied");
            }
            return;
        }
        if (WalletOwnerType.ORGANIZATION.equals(wallet.getOwnerType())) {
            if (currentAccountPortIn.hasPermission(TX_READ_ALL)) {
                return;
            }
            currentAccountPortIn.requirePermission(TX_READ_PARTNER);
            UUID accountId = currentAccountPortIn.getCurrentAccountIdOrThrow();
            if (!organizationPortOut.findActiveOrganizationIdsByAccountId(accountId)
                    .contains(wallet.getOrganizationId())) {
                throw new AccessDeniedException("Access is denied");
            }
            return;
        }
        currentAccountPortIn.requirePermission(TX_READ_ALL);
    }
}
