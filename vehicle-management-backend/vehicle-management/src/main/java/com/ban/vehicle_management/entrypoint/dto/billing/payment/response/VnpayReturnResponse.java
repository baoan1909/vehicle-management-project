package com.ban.vehicle_management.entrypoint.dto.billing.payment.response;

import com.ban.vehicle_management.shared.enumeration.billing.PaymentStatus;

public record VnpayReturnResponse(
        boolean validSignature,
        boolean successful,
        String transactionRef,
        String responseCode,
        String transactionStatus,
        PaymentStatus paymentStatus
) {
}
