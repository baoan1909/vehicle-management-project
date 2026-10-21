package com.ban.vehicle_management.infrastructure.persistence.database.entity.billing;

import com.ban.vehicle_management.shared.enumeration.billing.LedgerEntrySide;
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
@Table(name = "ledger_entries", schema = "billing")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LedgerEntryEntity {

    @Id
    @Column(name = "ledger_entry_id", nullable = false)
    private UUID ledgerEntryId;

    @Column(name = "financial_transaction_id", nullable = false)
    private UUID financialTransactionId;

    @Column(name = "ledger_account_id", nullable = false)
    private UUID ledgerAccountId;

    @Enumerated(EnumType.STRING)
    @Column(name = "entry_side", nullable = false)
    private LedgerEntrySide entrySide;

    @Column(name = "amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(name = "balance_after", precision = 19, scale = 2)
    private BigDecimal balanceAfter;

    @Column(name = "description")
    private String description;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
