package com.ban.vehicle_management.infrastructure.persistence.database.repository.billing;

import com.ban.vehicle_management.infrastructure.persistence.database.entity.billing.LedgerEntryEntity;
import java.util.List;
import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface LedgerEntryRepository extends JpaRepository<LedgerEntryEntity, UUID> {

    interface AccountNetProjection {
        String getAccountCode();
        BigDecimal getNetAmount();
    }

    @Query(value = """
            SELECT account.account_code AS "accountCode",
                   COALESCE(SUM(CASE WHEN entry.entry_side = 'DEBIT'
                                     THEN entry.amount ELSE -entry.amount END), 0) AS "netAmount"
            FROM billing.ledger_accounts account
            LEFT JOIN billing.ledger_entries entry
              ON entry.ledger_account_id = account.ledger_account_id
            GROUP BY account.account_code
            """, nativeQuery = true)
    List<AccountNetProjection> sumNetByAccountCode();

    List<LedgerEntryEntity> findByFinancialTransactionIdOrderByCreatedAtAsc(UUID financialTransactionId);
}
