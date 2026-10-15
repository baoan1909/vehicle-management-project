package com.ban.vehicle_management.domain.billing.wallet.model;

import com.ban.vehicle_management.shared.enumeration.billing.WalletAdjustmentStatus;
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
public class WalletAdjustment {

    private UUID walletAdjustmentId;
    private UUID walletId;
    private BigDecimal amount;
    /** CREDIT increases available balance, DEBIT decreases it. */
    private String direction;
    private String reason;
    private WalletAdjustmentStatus status;
    private UUID requestedBy;
    private Instant requestedAt;
    private UUID decidedBy;
    private Instant decidedAt;
    private UUID financialTransactionId;
}
