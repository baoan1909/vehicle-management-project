package com.ban.vehicle_management.infrastructure.persistence.adapter.billing;

import com.ban.vehicle_management.application.billing.wallet.port.out.SettlementPortOut;
import com.ban.vehicle_management.domain.billing.wallet.model.PartnerBankAccount;
import com.ban.vehicle_management.domain.billing.wallet.model.PayoutRequest;
import com.ban.vehicle_management.domain.billing.wallet.model.RevenueAllocation;
import com.ban.vehicle_management.domain.billing.wallet.model.VoucherFinancialTerm;
import com.ban.vehicle_management.infrastructure.mapper.billing.WalletPersistenceMapper;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.billing.LedgerEntryRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.billing.PartnerBankAccountRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.billing.PayoutRequestRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.billing.RevenueAllocationRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.billing.VoucherFinancialTermRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class SettlementPersistenceAdapter implements SettlementPortOut {

    private final RevenueAllocationRepository allocationRepository;
    private final VoucherFinancialTermRepository termRepository;
    private final PartnerBankAccountRepository bankAccountRepository;
    private final PayoutRequestRepository payoutRepository;
    private final WalletPersistenceMapper mapper;
    private final JdbcTemplate jdbcTemplate;

    public SettlementPersistenceAdapter(
            RevenueAllocationRepository allocationRepository,
            VoucherFinancialTermRepository termRepository,
            PartnerBankAccountRepository bankAccountRepository,
            PayoutRequestRepository payoutRepository,
            WalletPersistenceMapper mapper,
            JdbcTemplate jdbcTemplate) {
        this.allocationRepository = allocationRepository;
        this.termRepository = termRepository;
        this.bankAccountRepository = bankAccountRepository;
        this.payoutRepository = payoutRepository;
        this.mapper = mapper;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public RevenueAllocation saveAllocation(RevenueAllocation allocation) {
        return mapper.toDomain(allocationRepository.saveAndFlush(mapper.toEntity(allocation)));
    }

    @Override
    public Optional<RevenueAllocation> findAllocationByPayment(UUID paymentId) {
        return allocationRepository.findByPaymentId(paymentId).map(mapper::toDomain);
    }

    @Override
    public Optional<RevenueAllocation> findAllocationByIdForUpdate(UUID allocationId) {
        return allocationRepository.findByIdForUpdate(allocationId).map(mapper::toDomain);
    }

    @Override
    @Transactional
    public List<UUID> claimDueAllocations(int batchSize, Instant now) {
        return allocationRepository.claimDueAllocations(now, batchSize);
    }

    @Override
    public Optional<RevenueAllocation> findAllocationById(UUID allocationId) {
        return allocationRepository.findById(allocationId).map(mapper::toDomain);
    }

    @Override
    public List<RevenueAllocation> findByOrganization(UUID organizationId, int page, int size) {
        return allocationRepository.findByOrganizationIdOrderByCreatedAtDesc(
                organizationId, PageRequest.of(page, size)).stream().map(mapper::toDomain).toList();
    }

    @Override
    public Optional<VoucherFinancialTerm> findActiveTerm(UUID voucherId, Instant at) {
        if (voucherId == null) {
            return Optional.empty();
        }
        return termRepository.findActiveTerm(voucherId, at).map(mapper::toDomain);
    }

    @Override
    public VoucherFinancialTerm saveTerm(VoucherFinancialTerm term) {
        return mapper.toDomain(termRepository.saveAndFlush(mapper.toEntity(term)));
    }

    @Override
    public PartnerBankAccount saveBankAccount(PartnerBankAccount account) {
        return mapper.toDomain(bankAccountRepository.saveAndFlush(mapper.toEntity(account)));
    }

    @Override
    public Optional<PartnerBankAccount> findBankAccount(UUID bankAccountId) {
        return bankAccountRepository.findById(bankAccountId).map(mapper::toDomain);
    }

    @Override
    public List<PartnerBankAccount> findBankAccountsByOrg(UUID organizationId) {
        return bankAccountRepository.findByOrganizationIdOrderByVerifiedDesc(organizationId).stream()
                .map(mapper::toDomain).toList();
    }

    @Override
    public PayoutRequest savePayout(PayoutRequest payout) {
        return mapper.toDomain(payoutRepository.saveAndFlush(mapper.toEntity(payout)));
    }

    @Override
    public Optional<PayoutRequest> findPayoutById(UUID payoutId) {
        return payoutRepository.findById(payoutId).map(mapper::toDomain);
    }

    @Override
    public Optional<PayoutRequest> findPayoutByIdForUpdate(UUID payoutId) {
        return payoutRepository.findByIdForUpdate(payoutId).map(mapper::toDomain);
    }

    @Override
    public Optional<PayoutRequest> findPayoutByIdempotency(String key) {
        return payoutRepository.findByIdempotencyKey(key).map(mapper::toDomain);
    }

    @Override
    public List<PayoutRequest> findPayoutsByOrg(UUID organizationId, int page, int size) {
        return payoutRepository.findByOrganizationIdOrderByRequestedAtDesc(
                organizationId, PageRequest.of(page, size)).stream().map(mapper::toDomain).toList();
    }

    @Override
    public Map<String, BigDecimal> sumLedgerByAccount() {
        Map<String, BigDecimal> result = new HashMap<>();
        try {
            jdbcTemplate.query(
                    "SELECT a.account_code, COALESCE(SUM(CASE WHEN e.entry_side='DEBIT' THEN e.amount ELSE -e.amount END),0) AS net"
                            + " FROM billing.ledger_accounts a LEFT JOIN billing.ledger_entries e"
                            + " ON e.ledger_account_id = a.ledger_account_id GROUP BY a.account_code",
                    rs -> {
                        result.put(rs.getString(1), rs.getBigDecimal(2));
                    });
        } catch (Exception ignored) {
        }
        return result;
    }
}
