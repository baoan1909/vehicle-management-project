package com.ban.vehicle_management.application.billing.wallet.port.out;

import com.ban.vehicle_management.domain.billing.wallet.model.FinancialTransaction;
import com.ban.vehicle_management.domain.billing.wallet.model.LedgerAccount;
import com.ban.vehicle_management.domain.billing.wallet.model.LedgerEntry;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LedgerPortOut {

    FinancialTransaction saveTransaction(FinancialTransaction transaction);

    Optional<FinancialTransaction> findTransactionByIdempotencyKey(String idempotencyKey);

    Optional<FinancialTransaction> findTransactionById(UUID transactionId);

    List<FinancialTransaction> findTransactionsByWallet(UUID walletId, int page, int size);

    LedgerAccount saveAccount(LedgerAccount account);

    Optional<LedgerAccount> findAccountByCode(String accountCode);

    Optional<LedgerAccount> findAccountById(UUID accountId);

    LedgerEntry saveEntry(LedgerEntry entry);

    List<LedgerEntry> findEntriesByTransaction(UUID transactionId);

    void postBalancedTransaction(FinancialTransaction transaction, List<LedgerEntry> entries);
}
