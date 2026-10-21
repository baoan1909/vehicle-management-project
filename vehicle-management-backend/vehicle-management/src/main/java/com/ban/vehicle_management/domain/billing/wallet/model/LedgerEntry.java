package com.ban.vehicle_management.domain.billing.wallet.model;

import com.ban.vehicle_management.shared.enumeration.billing.LedgerEntrySide;
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
public class LedgerEntry {

    private UUID ledgerEntryId;
    private UUID financialTransactionId;
    private UUID ledgerAccountId;
    private LedgerEntrySide entrySide;
    private BigDecimal amount;
    private BigDecimal balanceAfter;
    private String description;
    private Instant createdAt;
}
