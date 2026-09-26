package com.ban.vehicle_management.application.accesscontrol.subscription.authorization;

import com.ban.vehicle_management.application.iam.account.port.in.CurrentAccountPortIn;
import com.ban.vehicle_management.application.iam.account.port.out.AccountProfilePortOut;
import com.ban.vehicle_management.application.iam.organization.authorization.OrganizationAccessGuard;
import com.ban.vehicle_management.application.parking.parkinglot.port.out.ParkingLotPortOut;
import com.ban.vehicle_management.domain.accesscontrol.subscription.model.Subscription;
import com.ban.vehicle_management.domain.iam.account.model.AccountProfileState;
import com.ban.vehicle_management.shared.enumeration.people.CustomerApprovalStatus;
import com.ban.vehicle_management.shared.enumeration.people.CustomerStatus;
import java.util.UUID;
import java.util.Set;
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
    private final OrganizationAccessGuard organizationAccessGuard;
    private final ParkingLotPortOut parkingLotPortOut;

    public SubscriptionAccessGuard(
            CurrentAccountPortIn currentAccountPortIn,
            AccountProfilePortOut accountProfilePortOut,
            OrganizationAccessGuard organizationAccessGuard,
            ParkingLotPortOut parkingLotPortOut
    ) {
        this.currentAccountPortIn = currentAccountPortIn;
        this.accountProfilePortOut = accountProfilePortOut;
        this.organizationAccessGuard = organizationAccessGuard;
        this.parkingLotPortOut = parkingLotPortOut;
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
            ensureLotVisible(subscription.getParkingLotId());
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

    /** Null is unrestricted only for platform scope or an own-customer query. */
    public Set<UUID> visibleParkingLotIdsForList() {
        if (!currentAccountPortIn.hasPermission(READ_ALL)) {
            return null;
        }
        return organizationAccessGuard.resolveAccessibleParkingLotIds(parkingLotPortOut);
    }

    public void ensureCanUpdate(Subscription subscription) {
        if (currentAccountPortIn.hasPermission(UPDATE_ALL)) {
            ensureCanOperate(subscription);
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
            ensureCanOperate(subscription);
            return;
        }

        currentAccountPortIn.requirePermission(CANCEL_OWN);
        UUID customerId = resolveCurrentApprovedCustomerId();

        if (!customerId.equals(subscription.getCustomerId())) {
            throw new AccessDeniedException("Access is denied");
        }
    }

    public void ensureCanCreateAtParkingLot(UUID parkingLotId) {
        ensureLotOperable(parkingLotId);
    }

    public void ensureCanOperate(Subscription subscription) {
        ensureLotOperable(subscription.getParkingLotId());
    }

    private void ensureLotVisible(UUID parkingLotId) {
        Set<UUID> allowed = organizationAccessGuard.resolveAccessibleParkingLotIds(parkingLotPortOut);
        if (allowed != null && (parkingLotId == null || !allowed.contains(parkingLotId))) {
            throw new AccessDeniedException("Current account cannot access this parking lot subscription");
        }
    }

    private void ensureLotOperable(UUID parkingLotId) {
        Set<String> permissions = currentAccountPortIn.getCurrentAccountOrThrow().getEffectivePermissionCodes();
        if (!permissions.contains(OrganizationAccessGuard.PARKING_SCOPE_PARTNER)
                && !permissions.contains(OrganizationAccessGuard.PARKING_SCOPE_ASSIGNED)) {
            throw new AccessDeniedException("Current account cannot operate parking lot subscriptions");
        }
        if (parkingLotId == null) {
            throw new AccessDeniedException("Subscription has no parking lot scope");
        }
        Set<UUID> allowed = organizationAccessGuard.resolveAccessibleParkingLotIds(parkingLotPortOut);
        if (allowed == null || !allowed.contains(parkingLotId)) {
            throw new AccessDeniedException("Current account cannot operate this parking lot subscription");
        }
    }
}
