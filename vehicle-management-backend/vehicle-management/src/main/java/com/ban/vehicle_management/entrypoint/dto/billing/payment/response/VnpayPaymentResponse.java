package com.ban.vehicle_management.entrypoint.dto.billing.payment.response;

import java.util.UUID;

public record VnpayPaymentResponse(
        UUID paymentId,
        UUID invoiceId,
        String transactionRef,
        String paymentUrl,
        String expiresAt
) {
}
