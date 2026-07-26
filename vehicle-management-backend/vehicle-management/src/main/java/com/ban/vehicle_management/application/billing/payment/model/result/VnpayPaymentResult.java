package com.ban.vehicle_management.application.billing.payment.model.result;

import java.time.Instant;
import java.util.UUID;

public record VnpayPaymentResult(
        UUID paymentId,
        UUID invoiceId,
        String transactionRef,
        String paymentUrl,
        Instant expiresAt
) {
}
