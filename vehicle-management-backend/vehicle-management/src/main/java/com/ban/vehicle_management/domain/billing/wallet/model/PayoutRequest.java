package com.ban.vehicle_management.domain.billing.wallet.model;

import com.ban.vehicle_management.shared.enumeration.billing.PayoutStatus;
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
public class PayoutRequest {

    private UUID payoutRequestId;
    private UUID walletId;
    private UUID organizationId;
    private UUID bankAccountId;
    private BigDecimal amount;
    private String currency;
    private PayoutStatus status;
    private String idempotencyKey;
    private UUID requestedBy;
    private Instant requestedAt;
    private UUID decidedBy;
    private Instant decidedAt;
    private String failureReason;
    private UUID financialTransactionId;
}
