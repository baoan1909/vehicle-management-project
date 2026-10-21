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
public class WalletResponse {

    private UUID walletId;
    private String ownerType;
    private UUID customerId;
    private UUID organizationId;
    private String walletPurpose;
    private String currency;
    private BigDecimal availableBalance;
    private BigDecimal pendingBalance;
    private BigDecimal heldBalance;
    private String status;
    private Instant updatedAt;
}
