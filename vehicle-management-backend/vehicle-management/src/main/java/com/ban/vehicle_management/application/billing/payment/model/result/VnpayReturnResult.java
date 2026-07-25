package com.ban.vehicle_management.application.billing.payment.model.result;

import com.ban.vehicle_management.shared.enumeration.billing.PaymentStatus;

public record VnpayReturnResult(
        boolean validSignature,
        boolean successful,
        String transactionRef,
        String responseCode,
        String transactionStatus,
        PaymentStatus paymentStatus
) {
}
