package com.ban.vehicle_management.application.accesscontrol.subscription.authorization;

import com.ban.vehicle_management.application.iam.account.port.in.CurrentAccountPortIn;
import com.ban.vehicle_management.application.iam.account.port.out.AccountProfilePortOut;
import com.ban.vehicle_management.domain.accesscontrol.subscription.model.Subscription;
import com.ban.vehicle_management.domain.iam.account.model.AccountProfileState;
import com.ban.vehicle_management.shared.enumeration.people.CustomerApprovalStatus;
import com.ban.vehicle_management.shared.enumeration.people.CustomerStatus;
import java.util.UUID;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

@Component
public class SubscriptionAccessGuard {

    public static final String CREATE_OWN = "SUBSCRIPTION_CREATE_OWN";
    public static final String CREATE_ALL = "SUBSCRIPTION_CREATE_ALL";
    public static final String READ_OWN = "SUBSCRIPTION_READ_OWN";
    public static final String READ_ALL = "SUBSCRIPTION_READ_ALL";
    public static final String UPDATE_OWN = "SUBSCRIPTION_UPDATE_OWN";
    public static final String UPDATE_ALL = "SUBSCRIPTION_UPDATE_ALL";
    public static final String APPROVE_ALL = "SUBSCRIPTION_APPROVE_ALL";
    public static final String REJECT_ALL = "SUBSCRIPTION_REJECT_ALL";
    public static final String CANCEL_OWN = "SUBSCRIPTION_CANCEL_OWN";
    public static final String CANCEL_ALL = "SUBSCRIPTION_CANCEL_ALL";
    public static final String ASSIGN_CARD_ALL = "SUBSCRIPTION_ASSIGN_CARD_ALL";
    public static final String EXPIRE_ALL = "SUBSCRIPTION_EXPIRE_ALL";

    private final CurrentAccountPortIn currentAccountPortIn;
    private final AccountProfilePortOut accountProfilePortOut;

    public SubscriptionAccessGuard(
            CurrentAccountPortIn currentAccountPortIn,
            AccountProfilePortOut accountProfilePortOut
    ) {
        this.currentAccountPortIn = currentAccountPortIn;
        this.accountProfilePortOut = accountProfilePortOut;
    }

    public UUID resolveCurrentApprovedCustomerId() {
        UUID accountId = currentAccountPortIn.getCurrentAccountIdOrThrow();

        AccountProfileState profileState = accountProfilePortOut.findProfileStateByAccountId(accountId)
                .orElseThrow(() -> new AccessDeniedException("Access is denied"));

        if (profileState.customerId() == null
                || !CustomerStatus.ACTIVE.equals(profileState.customerStatus())
                || !CustomerApprovalStatus.APPROVED.equals(profileState.customerApprovalStatus())) {
            throw new AccessDeniedException("Access is denied");
        }

        return profileState.customerId();
    }

    public void ensureCanCreateOwn() {
        currentAccountPortIn.requirePermission(CREATE_OWN);
    }

    public void ensureCanCreateAll() {
        currentAccountPortIn.requirePermission(CREATE_ALL);
    }

    public void ensureCanApprove() {
        currentAccountPortIn.requirePermission(APPROVE_ALL);
    }

    public void ensureCanReject() {
        currentAccountPortIn.requirePermission(REJECT_ALL);
    }

    public void ensureCanAssignCard() {
        currentAccountPortIn.requirePermission(ASSIGN_CARD_ALL);
    }

    public void ensureCanExpire() {
        currentAccountPortIn.requirePermission(EXPIRE_ALL);
    }

    public void ensureCanRead(Subscription subscription) {
        if (currentAccountPortIn.hasPermission(READ_ALL)) {
            return;
        }

        currentAccountPortIn.requirePermission(READ_OWN);
        UUID customerId = resolveCurrentApprovedCustomerId();

        if (!customerId.equals(subscription.getCustomerId())) {
            throw new AccessDeniedException("Access is denied");
        }
    }

    public UUID resolveCustomerIdForList(UUID requestedCustomerId) {
        if (currentAccountPortIn.hasPermission(READ_ALL)) {
            return requestedCustomerId;
        }

        currentAccountPortIn.requirePermission(READ_OWN);
        UUID currentCustomerId = resolveCurrentApprovedCustomerId();

        if (requestedCustomerId != null && !currentCustomerId.equals(requestedCustomerId)) {
            throw new AccessDeniedException("Access is denied");
        }

        return currentCustomerId;
    }

    public void ensureCanUpdate(Subscription subscription) {
        if (currentAccountPortIn.hasPermission(UPDATE_ALL)) {
            return;
        }

        currentAccountPortIn.requirePermission(UPDATE_OWN);
        UUID customerId = resolveCurrentApprovedCustomerId();

        if (!customerId.equals(subscription.getCustomerId())) {
            throw new AccessDeniedException("Access is denied");
        }
    }

    public void ensureCanCancel(Subscription subscription) {
        if (currentAccountPortIn.hasPermission(CANCEL_ALL)) {
            return;
        }

        currentAccountPortIn.requirePermission(CANCEL_OWN);
        UUID customerId = resolveCurrentApprovedCustomerId();

        if (!customerId.equals(subscription.getCustomerId())) {
            throw new AccessDeniedException("Access is denied");
        }
    }
}