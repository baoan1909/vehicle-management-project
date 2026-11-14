package com.ban.vehicle_management.application.billing.payment.model.result;

import java.time.Instant;

public record SepayPaymentInstruction(
        String qrCodeUrl,
        String bankCode,
        String accountNumber,
        String accountHolder,
        String transferContent,
        Instant expiresAt
) { }
