package com.ban.vehicle_management.application.billing.payment.authorization;

import com.ban.vehicle_management.application.iam.account.port.in.CurrentAccountPortIn;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class PaymentAccessGuard {

    private static final String CREATE_PERMISSION = "PAYMENT_CREATE_ALL";
    private static final String READ_PERMISSION = "PAYMENT_READ_ALL";

    private final CurrentAccountPortIn currentAccountPortIn;

    public PaymentAccessGuard(CurrentAccountPortIn currentAccountPortIn) {
        this.currentAccountPortIn = currentAccountPortIn;
    }

    public UUID requireCanCreateAndGetAccountId() {
        currentAccountPortIn.requirePermission(CREATE_PERMISSION);
        return currentAccountPortIn.getCurrentAccountIdOrThrow();
    }

    public void ensureCanReadAll() {
        currentAccountPortIn.requirePermission(READ_PERMISSION);
    }
}