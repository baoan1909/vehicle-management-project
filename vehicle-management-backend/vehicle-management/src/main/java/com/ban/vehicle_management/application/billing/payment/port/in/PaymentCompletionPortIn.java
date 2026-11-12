package com.ban.vehicle_management.application.billing.payment.port.in;

import java.util.UUID;

public interface PaymentCompletionPortIn {

    void completePaymentTruth(UUID paymentId);
}
