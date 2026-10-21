package com.ban.vehicle_management.domain.billing.wallet.model;

import com.ban.vehicle_management.shared.enumeration.billing.WalletTopupStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class WalletTopupOrder {

    private UUID topupOrderId;
    private UUID walletId;
    private BigDecimal amount;
    private String currency;
    private WalletTopupStatus status;
    private String transactionRef;
    private String providerTransactionNo;
    private String providerResponseCode;
    private String providerTransactionStatus;
    private String paymentUrl;
    private Instant expiresAt;
    private Instant completedAt;
    private String failureReason;
    private String idempotencyKey;
    private Instant createdAt;
    private UUID createdBy;
    private Instant updatedAt;
    private UUID updatedBy;
}
