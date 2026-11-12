package com.ban.vehicle_management.infrastructure.persistence.adapter.billing;

import com.ban.vehicle_management.application.billing.wallet.port.out.SettlementPortOut;
import com.ban.vehicle_management.domain.billing.wallet.model.PartnerBankAccount;
import com.ban.vehicle_management.domain.billing.wallet.model.PayoutRequest;
import com.ban.vehicle_management.domain.billing.wallet.model.RevenueAllocation;
import com.ban.vehicle_management.domain.billing.wallet.model.RevenueAllocationReversal;
import com.ban.vehicle_management.domain.billing.wallet.model.RevenueAllocationReversalTotals;
import com.ban.vehicle_management.domain.billing.wallet.model.VoucherFinancialTerm;
import com.ban.vehicle_management.infrastructure.mapper.billing.WalletPersistenceMapper;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.billing.LedgerEntryRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.billing.PartnerBankAccountRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.billing.PayoutRequestRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.billing.RevenueAllocationRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.billing.RevenueAllocationReversalRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.billing.VoucherFinancialTermRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class SettlementPersistenceAdapter implements SettlementPortOut {

    private final RevenueAllocationRepository allocationRepository;
    private final RevenueAllocationReversalRepository reversalRepository;
    private final VoucherFinancialTermRepository termRepository;
    private final PartnerBankAccountRepository bankAccountRepository;
    private final PayoutRequestRepository payoutRepository;
    private final LedgerEntryRepository ledgerEntryRepository;
    private final WalletPersistenceMapper mapper;

    public SettlementPersistenceAdapter(
            RevenueAllocationRepository allocationRepository,
            RevenueAllocationReversalRepository reversalRepository,
            VoucherFinancialTermRepository termRepository,
            PartnerBankAccountRepository bankAccountRepository,
            PayoutRequestRepository payoutRepository,
            LedgerEntryRepository ledgerEntryRepository,
            WalletPersistenceMapper mapper) {
        this.allocationRepository = allocationRepository;
        this.reversalRepository = reversalRepository;
        this.termRepository = termRepository;
        this.bankAccountRepository = bankAccountRepository;
        this.payoutRepository = payoutRepository;
        this.ledgerEntryRepository = ledgerEntryRepository;
        this.mapper = mapper;
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
    public Optional<RevenueAllocationReversal> findReversalByRefundPayment(UUID refundPaymentId) {
        return reversalRepository.findByRefundPaymentId(refundPaymentId).map(mapper::toDomain);
    }

    @Override
    public RevenueAllocationReversalTotals sumReversals(UUID allocationId) {
        List<RevenueAllocationReversal> reversals = reversalRepository
                .findByRevenueAllocationId(allocationId).stream().map(mapper::toDomain).toList();
        return new RevenueAllocationReversalTotals(
                sum(reversals, RevenueAllocationReversal::getRefundAmount),
                sum(reversals, RevenueAllocationReversal::getPlatformFeeAmount),
                sum(reversals, RevenueAllocationReversal::getVoucherCommissionAmount),
                sum(reversals, RevenueAllocationReversal::getPartnerPayableAmount),
                sum(reversals, RevenueAllocationReversal::getGatewayFeeAmount),
                sum(reversals, RevenueAllocationReversal::getDiscountAmount));
    }

    @Override
    public RevenueAllocationReversal saveReversal(RevenueAllocationReversal reversal) {
        return mapper.toDomain(reversalRepository.saveAndFlush(mapper.toEntity(reversal)));
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
        return ledgerEntryRepository.sumNetByAccountCode().stream()
                .collect(java.util.stream.Collectors.toMap(
                        LedgerEntryRepository.AccountNetProjection::getAccountCode,
                        LedgerEntryRepository.AccountNetProjection::getNetAmount));
    }

    private BigDecimal sum(List<RevenueAllocationReversal> reversals,
            java.util.function.Function<RevenueAllocationReversal, BigDecimal> amount) {
        return reversals.stream().map(amount).filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
