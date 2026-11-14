package com.ban.vehicle_management.entrypoint.dto.billing.payment.response;

import java.time.Instant;
import java.util.UUID;

public record SepayPaymentResponse(
        UUID paymentId,
        UUID invoiceId,
        String transactionRef,
        String qrCodeUrl,
        String bankCode,
        String accountNumber,
        String accountHolder,
        Instant expiresAt
) { }
