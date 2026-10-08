package com.ban.vehicle_management.infrastructure.persistence.adapter.billing;

import com.ban.vehicle_management.application.billing.wallet.port.out.LedgerPortOut;
import com.ban.vehicle_management.domain.billing.wallet.model.FinancialTransaction;
import com.ban.vehicle_management.domain.billing.wallet.model.LedgerAccount;
import com.ban.vehicle_management.domain.billing.wallet.model.LedgerEntry;
import com.ban.vehicle_management.domain.billing.wallet.policy.LedgerPostingPolicy;
import com.ban.vehicle_management.infrastructure.mapper.billing.WalletPersistenceMapper;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.billing.FinancialTransactionRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.billing.LedgerAccountRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.billing.LedgerEntryRepository;
import com.ban.vehicle_management.shared.exception.ConflictException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class LedgerPersistenceAdapter implements LedgerPortOut {

    private final FinancialTransactionRepository transactionRepository;
    private final LedgerAccountRepository accountRepository;
    private final LedgerEntryRepository entryRepository;
    private final WalletPersistenceMapper mapper;
    private final LedgerPostingPolicy postingPolicy = new LedgerPostingPolicy();

    public LedgerPersistenceAdapter(
            FinancialTransactionRepository transactionRepository,
            LedgerAccountRepository accountRepository,
            LedgerEntryRepository entryRepository,
            WalletPersistenceMapper mapper) {
        this.transactionRepository = transactionRepository;
        this.accountRepository = accountRepository;
        this.entryRepository = entryRepository;
        this.mapper = mapper;
    }

    @Override
    public FinancialTransaction saveTransaction(FinancialTransaction transaction) {
        try {
            return mapper.toDomain(
                    transactionRepository.saveAndFlush(mapper.toEntity(transaction)));
        } catch (DataIntegrityViolationException exception) {
            throw new ConflictException("Financial transaction already exists");
        }
    }

    @Override
    public Optional<FinancialTransaction> findTransactionByIdempotencyKey(String idempotencyKey) {
        return transactionRepository.findByIdempotencyKey(idempotencyKey).map(mapper::toDomain);
    }

    @Override
    public Optional<FinancialTransaction> findTransactionById(UUID transactionId) {
        return transactionRepository.findById(transactionId).map(mapper::toDomain);
    }

    @Override
    public List<FinancialTransaction> findTransactionsByWallet(UUID walletId, int page, int size) {
        return transactionRepository
                .findByWalletReference("WALLET", walletId, PageRequest.of(page, size))
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public LedgerAccount saveAccount(LedgerAccount account) {
        return mapper.toDomain(accountRepository.saveAndFlush(mapper.toEntity(account)));
    }

    @Override
    public Optional<LedgerAccount> findAccountByCode(String accountCode) {
        return accountRepository.findByAccountCode(accountCode).map(mapper::toDomain);
    }

    @Override
    public Optional<LedgerAccount> findAccountById(UUID accountId) {
        return accountRepository.findById(accountId).map(mapper::toDomain);
    }

    @Override
    public LedgerEntry saveEntry(LedgerEntry entry) {
        return mapper.toDomain(entryRepository.saveAndFlush(mapper.toEntity(entry)));
    }

    @Override
    public List<LedgerEntry> findEntriesByTransaction(UUID transactionId) {
        return entryRepository.findByFinancialTransactionIdOrderByCreatedAtAsc(transactionId).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    @Transactional
    public void postBalancedTransaction(FinancialTransaction transaction, List<LedgerEntry> entries) {
        postingPolicy.requireBalanced(entries);
        saveTransaction(transaction);
        for (LedgerEntry entry : entries) {
            entry.setFinancialTransactionId(transaction.getFinancialTransactionId());
            saveEntry(entry);
        }
    }
}
