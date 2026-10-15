package com.ban.vehicle_management.entrypoint.dto.billing.wallet.response;

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
public class WalletAdjustmentResponse {

    private UUID walletAdjustmentId;
    private UUID walletId;
    private BigDecimal amount;
    private String direction;
    private String reason;
    private String status;
    private UUID requestedBy;
    private Instant requestedAt;
    private UUID decidedBy;
    private Instant decidedAt;
    private UUID financialTransactionId;
}
