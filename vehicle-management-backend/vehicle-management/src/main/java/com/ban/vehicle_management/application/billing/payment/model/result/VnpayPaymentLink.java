package com.ban.vehicle_management.application.billing.payment.model.result;

import java.time.Instant;

public record VnpayPaymentLink(
        String paymentUrl,
        Instant expiresAt
) {
}
