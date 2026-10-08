package com.ban.vehicle_management.domain.billing.wallet.model;

import com.ban.vehicle_management.shared.enumeration.billing.WalletOwnerType;
import com.ban.vehicle_management.shared.enumeration.billing.WalletPurpose;
import com.ban.vehicle_management.shared.enumeration.billing.WalletStatus;
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
public class Wallet {

    private UUID walletId;
    private WalletOwnerType ownerType;
    private UUID customerId;
    private UUID organizationId;
    private WalletPurpose walletPurpose;
    private String currency;
    private BigDecimal availableBalance;
    private BigDecimal pendingBalance;
    private BigDecimal heldBalance;
    private WalletStatus status;
    private long version;
    private Instant createdAt;
    private UUID createdBy;
    private Instant updatedAt;
    private UUID updatedBy;
}
