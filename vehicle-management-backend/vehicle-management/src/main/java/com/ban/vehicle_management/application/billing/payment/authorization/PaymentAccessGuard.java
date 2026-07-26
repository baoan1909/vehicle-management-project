package com.ban.vehicle_management.application.billing.payment.authorization;

import com.ban.vehicle_management.application.iam.account.port.in.CurrentAccountPortIn;
import com.ban.vehicle_management.application.iam.account.port.out.AccountProfilePortOut;
import com.ban.vehicle_management.domain.billing.invoice.model.Invoice;
import com.ban.vehicle_management.domain.iam.account.model.AccountProfileState;
import com.ban.vehicle_management.shared.enumeration.people.CustomerApprovalStatus;
import com.ban.vehicle_management.shared.enumeration.people.CustomerStatus;
import java.util.UUID;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

@Component
public class PaymentAccessGuard {

    private static final String CREATE_PERMISSION = "PAYMENT_CREATE_ALL";
    private static final String CREATE_OWN_PERMISSION = "PAYMENT_CREATE_OWN";
    private static final String READ_PERMISSION = "PAYMENT_READ_ALL";

    private final CurrentAccountPortIn currentAccountPortIn;
    private final AccountProfilePortOut accountProfilePortOut;

    public PaymentAccessGuard(
            CurrentAccountPortIn currentAccountPortIn,
            AccountProfilePortOut accountProfilePortOut
    ) {
        this.currentAccountPortIn = currentAccountPortIn;
        this.accountProfilePortOut = accountProfilePortOut;
    }

    public UUID requireCanCreateAndGetAccountId() {
        currentAccountPortIn.requirePermission(CREATE_PERMISSION);
        return currentAccountPortIn.getCurrentAccountIdOrThrow();
    }

    public void ensureCanReadAll() {
        currentAccountPortIn.requirePermission(READ_PERMISSION);
    }

    public void ensureCanCreateVnpayPayment(Invoice invoice) {
        if (currentAccountPortIn.hasPermission(CREATE_PERMISSION)) {
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
