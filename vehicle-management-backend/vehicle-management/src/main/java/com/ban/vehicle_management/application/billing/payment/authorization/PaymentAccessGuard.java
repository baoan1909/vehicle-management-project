package com.ban.vehicle_management.application.billing.payment.authorization;

import com.ban.vehicle_management.application.iam.account.port.in.CurrentAccountPortIn;
import com.ban.vehicle_management.application.billing.payment.port.out.EmployeePaymentScopePortOut;
import com.ban.vehicle_management.application.iam.organization.authorization.OrganizationAccessGuard;
import com.ban.vehicle_management.application.parking.parkinglot.port.out.ParkingLotPortOut;
import com.ban.vehicle_management.application.iam.account.port.out.AccountProfilePortOut;
import com.ban.vehicle_management.domain.billing.invoice.model.Invoice;
import com.ban.vehicle_management.domain.iam.account.model.AccountProfileState;
import com.ban.vehicle_management.shared.enumeration.people.CustomerApprovalStatus;
import com.ban.vehicle_management.shared.enumeration.people.CustomerStatus;
import java.util.UUID;
import java.util.Set;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

@Component
public class PaymentAccessGuard {

    private static final String CREATE_PERMISSION = "PAYMENT_CREATE_ALL";
    private static final String CREATE_OWN_PERMISSION = "PAYMENT_CREATE_OWN";
    private static final String READ_PERMISSION = "PAYMENT_READ_ALL";
    private static final String SHIFT_SCOPE_PERMISSION = "PARKING_SCOPE_SHIFT";

    private final CurrentAccountPortIn currentAccountPortIn;
    private final AccountProfilePortOut accountProfilePortOut;
    private final OrganizationAccessGuard organizationAccessGuard;
    private final ParkingLotPortOut parkingLotPortOut;
    private final EmployeePaymentScopePortOut employeePaymentScopePortOut;

    public PaymentAccessGuard(
            CurrentAccountPortIn currentAccountPortIn,
            AccountProfilePortOut accountProfilePortOut,
            OrganizationAccessGuard organizationAccessGuard,
            ParkingLotPortOut parkingLotPortOut,
            EmployeePaymentScopePortOut employeePaymentScopePortOut
    ) {
        this.currentAccountPortIn = currentAccountPortIn;
        this.accountProfilePortOut = accountProfilePortOut;
        this.organizationAccessGuard = organizationAccessGuard;
        this.parkingLotPortOut = parkingLotPortOut;
        this.employeePaymentScopePortOut = employeePaymentScopePortOut;
    }

    public UUID requireCanCreateAndGetAccountId() {
        currentAccountPortIn.requirePermission(CREATE_PERMISSION);
        return currentAccountPortIn.getCurrentAccountIdOrThrow();
    }

    public void ensureCanReadAll() {
        currentAccountPortIn.requirePermission(READ_PERMISSION);
    }

    public Set<UUID> visibleParkingLotIdsForList() {
        ensureCanReadAll();
        return resolveVisibleParkingLotIds();
    }

    public void ensureCanRecordPayment(Invoice invoice) {
        Set<UUID> allowed = resolveVisibleParkingLotIds();
        if (invoice.getParkingLotId() == null || allowed == null
                || !allowed.contains(invoice.getParkingLotId())) {
            throw new AccessDeniedException("Current account cannot operate this parking lot invoice");
        }
    }

    private Set<UUID> resolveVisibleParkingLotIds() {
        var currentAccount = currentAccountPortIn.getCurrentAccountOrThrow();
        Set<String> permissions = currentAccount.getEffectivePermissionCodes();
        if (permissions.contains(OrganizationAccessGuard.PARKING_SCOPE_PLATFORM)
                || permissions.contains(OrganizationAccessGuard.PARKING_SCOPE_PARTNER)
                || permissions.contains(OrganizationAccessGuard.PARKING_SCOPE_ASSIGNED)) {
            return organizationAccessGuard.resolveAccessibleParkingLotIds(parkingLotPortOut);
        }
        if (permissions.contains(SHIFT_SCOPE_PERMISSION)) {
            return employeePaymentScopePortOut.findOpenShiftParkingLotIds(currentAccount.accountId());
        }
        throw new AccessDeniedException("Current account has no parking lot payment scope");
    }

    public void ensureCanCreateVnpayPayment(Invoice invoice) {
        if (currentAccountPortIn.hasPermission(CREATE_PERMISSION)) {
            ensureCanRecordPayment(invoice);
            return;
        }

        currentAccountPortIn.requirePermission(CREATE_OWN_PERMISSION);
        UUID currentCustomerId = resolveCurrentApprovedCustomerId();
        if (invoice.getCustomerId() == null || !currentCustomerId.equals(invoice.getCustomerId())) {
            throw new AccessDeniedException("Access is denied");
        }
    }

    private UUID resolveCurrentApprovedCustomerId() {
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
}
