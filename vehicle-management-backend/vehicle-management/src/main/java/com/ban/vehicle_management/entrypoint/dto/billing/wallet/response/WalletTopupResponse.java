package com.ban.vehicle_management.entrypoint.dto.billing.wallet.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record WalletTopupResponse(
        UUID topupOrderId,
        UUID walletId,
        BigDecimal amount,
        String currency,
        String status,
        String transactionRef,
        String paymentUrl,
        Instant expiresAt,
        Instant completedAt,
        String idempotencyKey
) {
}
