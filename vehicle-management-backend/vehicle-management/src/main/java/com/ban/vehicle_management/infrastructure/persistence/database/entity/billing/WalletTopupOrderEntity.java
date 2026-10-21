package com.ban.vehicle_management.infrastructure.persistence.database.entity.billing;

import com.ban.vehicle_management.shared.enumeration.billing.WalletTopupStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "wallet_topup_orders", schema = "billing")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class WalletTopupOrderEntity {

    @Id
    @Column(name = "topup_order_id", nullable = false)
    private UUID topupOrderId;

    @Column(name = "wallet_id", nullable = false)
    private UUID walletId;

    @Column(name = "amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private WalletTopupStatus status;

    @Column(name = "transaction_ref", nullable = false, unique = true)
    private String transactionRef;

    @Column(name = "provider_transaction_no")
    private String providerTransactionNo;

    @Column(name = "provider_response_code")
    private String providerResponseCode;

    @Column(name = "provider_transaction_status")
    private String providerTransactionStatus;

    @Column(name = "payment_url")
    private String paymentUrl;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "failure_reason")
    private String failureReason;

    @Column(name = "idempotency_key", nullable = false, unique = true)
    private String idempotencyKey;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @Column(name = "updated_by")
    private UUID updatedBy;
}
