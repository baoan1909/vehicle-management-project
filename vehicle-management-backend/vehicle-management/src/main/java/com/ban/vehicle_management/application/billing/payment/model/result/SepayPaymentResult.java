package com.ban.vehicle_management.application.billing.payment.model.result;

import java.time.Instant;
import java.util.UUID;

public record SepayPaymentResult(
        UUID paymentId,
        UUID invoiceId,
        String transactionRef,
        String qrCodeUrl,
        String bankCode,
        String accountNumber,
        String accountHolder,
        Instant expiresAt
) { }
