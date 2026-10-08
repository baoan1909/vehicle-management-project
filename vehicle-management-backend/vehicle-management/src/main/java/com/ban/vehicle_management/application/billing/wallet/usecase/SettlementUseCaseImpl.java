package com.ban.vehicle_management.application.billing.wallet.usecase;

import com.ban.vehicle_management.application.billing.invoice.port.out.InvoicePortOut;
import com.ban.vehicle_management.application.billing.payment.port.out.PaymentPortOut;
import com.ban.vehicle_management.application.billing.wallet.authorization.WalletAccessGuard;
import com.ban.vehicle_management.application.billing.wallet.port.in.SettlementPortIn;
import com.ban.vehicle_management.application.billing.wallet.port.out.LedgerPortOut;
import com.ban.vehicle_management.application.billing.wallet.port.out.SettlementPortOut;
import com.ban.vehicle_management.application.billing.wallet.port.out.WalletPortOut;
import com.ban.vehicle_management.application.iam.account.port.in.CurrentAccountPortIn;
import com.ban.vehicle_management.application.iam.organization.port.out.OrganizationPortOut;
import com.ban.vehicle_management.domain.billing.invoice.model.Invoice;
import com.ban.vehicle_management.domain.billing.payment.model.Payment;
import com.ban.vehicle_management.domain.billing.wallet.model.FinancialTransaction;
import com.ban.vehicle_management.domain.billing.wallet.model.LedgerAccount;
import com.ban.vehicle_management.domain.billing.wallet.model.LedgerEntry;
import com.ban.vehicle_management.domain.billing.wallet.model.PayoutRequest;
import com.ban.vehicle_management.domain.billing.wallet.model.RevenueAllocation;
import com.ban.vehicle_management.domain.billing.wallet.model.VoucherFinancialTerm;
import com.ban.vehicle_management.domain.billing.wallet.model.Wallet;
import com.ban.vehicle_management.domain.billing.wallet.policy.LedgerPostingPolicy;
import com.ban.vehicle_management.domain.billing.wallet.policy.RevenueAllocationPolicy;
import com.ban.vehicle_management.domain.billing.wallet.policy.WalletPolicy;
import com.ban.vehicle_management.shared.enumeration.billing.FinancialTransactionStatus;
import com.ban.vehicle_management.shared.enumeration.billing.FinancialTransactionType;
import com.ban.vehicle_management.shared.enumeration.billing.LedgerAccountType;
import com.ban.vehicle_management.shared.enumeration.billing.LedgerEntrySide;
import com.ban.vehicle_management.shared.enumeration.billing.PayoutStatus;
import com.ban.vehicle_management.shared.enumeration.billing.RevenueAllocationStatus;
import com.ban.vehicle_management.shared.enumeration.billing.WalletPurpose;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import com.ban.vehicle_management.shared.exception.ConflictException;
import com.ban.vehicle_management.shared.exception.NotFoundException;
import com.ban.vehicle_management.shared.utils.TextValidationUtils;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Phase 4: revenue allocation snapshot, 7-day settlement, payout with hold.
 */
@Service
public class SettlementUseCaseImpl implements SettlementPortIn {

    private final WalletPortOut walletPortOut;
    private final InvoicePortOut invoicePortOut;
    private final PaymentPortOut paymentPortOut;
    private final LedgerPortOut ledgerPortOut;
    private final SettlementPortOut settlementPortOut;
    private final OrganizationPortOut organizationPortOut;
    private final WalletAccessGuard walletAccessGuard;
    private final CurrentAccountPortIn currentAccountPortIn;
    private final BigDecimal platformFeeRate;
    private final int settlementDelayDays;
    private final RevenueAllocationPolicy allocationPolicy = new RevenueAllocationPolicy();
    private final LedgerPostingPolicy postingPolicy = new LedgerPostingPolicy();
    private final WalletPolicy walletPolicy = new WalletPolicy();

    public SettlementUseCaseImpl(
            WalletPortOut walletPortOut,
            InvoicePortOut invoicePortOut,
            PaymentPortOut paymentPortOut,
            LedgerPortOut ledgerPortOut,
            SettlementPortOut settlementPortOut,
            OrganizationPortOut organizationPortOut,
            WalletAccessGuard walletAccessGuard,
            CurrentAccountPortIn currentAccountPortIn,
            @Value("${app.wallet.platform-fee-rate:0.05}") BigDecimal platformFeeRate,
            @Value("${app.wallet.settlement-delay-days:7}") int settlementDelayDays) {
        this.walletPortOut = walletPortOut;
        this.invoicePortOut = invoicePortOut;
        this.paymentPortOut = paymentPortOut;
        this.ledgerPortOut = ledgerPortOut;
        this.settlementPortOut = settlementPortOut;
        this.organizationPortOut = organizationPortOut;
        this.walletAccessGuard = walletAccessGuard;
        this.currentAccountPortIn = currentAccountPortIn;
        this.platformFeeRate = platformFeeRate;
        this.settlementDelayDays = settlementDelayDays;
    }

    @Override
    @Transactional
    public RevenueAllocation allocateForWalletPayment(UUID paymentId, UUID voucherId) {
        currentAccountPortIn.requirePermission("WALLET_SETTLEMENT_PROCESS_ALL");
        Payment payment = paymentPortOut.findByIdForUpdate(paymentId)
                .orElseThrow(() -> new NotFoundException("Payment not found"));
        var existing = settlementPortOut.findAllocationByPayment(paymentId);
        if (existing.isPresent()) {
            return existing.get();
        }
        Invoice invoice = invoicePortOut.findById(payment.getInvoiceId())
                .orElseThrow(() -> new NotFoundException("Invoice not found"));
        UUID organizationId = resolveOrganizationId(invoice);
        VoucherFinancialTerm term = voucherId == null ? null
                : settlementPortOut.findActiveTerm(voucherId, Instant.now()).orElse(null);
        RevenueAllocation allocation = allocationPolicy.allocate(
                paymentId, invoice.getInvoiceId(), invoice.getParkingLotId(), organizationId, voucherId,
                invoice.getAmount(), invoice.getDiscountAmount(), invoice.getFinalAmount(),
                platformFeeRate, BigDecimal.ZERO, term, settlementDelayDays, payment.getPaidAt());

        // Partner wallet pending increases; ledger mirrors revenue split.
        if (organizationId != null) {
            Wallet partnerWallet = walletPortOut.findOrganizationWalletForUpdate(
                    organizationId, "VND", WalletPurpose.ORGANIZATION_SETTLEMENT)
                    .orElseThrow(() -> new NotFoundException("Partner wallet not found"));
            partnerWallet.setPendingBalance(
                    partnerWallet.getPendingBalance().add(allocation.getPartnerPayableAmount()));
            partnerWallet.setUpdatedAt(Instant.now());
            walletPortOut.save(partnerWallet);
        }
        postAllocationLedger(allocation, paymentId);
        try {
            return settlementPortOut.saveAllocation(allocation);
        } catch (DataIntegrityViolationException exception) {
            return settlementPortOut.findAllocationByPayment(paymentId)
                    .orElseThrow(() -> new ConflictException("Allocation already exists"));
        }
    }

    @Override
    @Transactional
    public int releaseDueSettlements(int batchSize) {
        List<UUID> claimed = settlementPortOut.claimDueAllocations(
                Math.max(1, Math.min(batchSize, 500)), Instant.now());
        int released = 0;
        for (UUID allocationId : claimed) {
            try {
                releaseOne(allocationId);
                released++;
            } catch (Exception ignored) {
            }
        }
        return released;
    }

    private void releaseOne(UUID allocationId) {
        RevenueAllocation allocation = settlementPortOut.findAllocationByIdForUpdate(allocationId)
                .orElseThrow(() -> new NotFoundException("Allocation not found"));
        if (!RevenueAllocationStatus.POSTED.equals(allocation.getStatus())
                || allocation.getSettledAt() != null) {
            return;
        }
        if (allocation.getOrganizationId() == null) {
            allocation.setSettledAt(Instant.now());
            settlementPortOut.saveAllocation(allocation);
            return;
        }
        Wallet partnerWallet = walletPortOut.findOrganizationWalletForUpdate(
                allocation.getOrganizationId(), "VND", WalletPurpose.ORGANIZATION_SETTLEMENT)
                .orElseThrow(() -> new NotFoundException("Partner wallet not found"));
        if (partnerWallet.getPendingBalance().compareTo(allocation.getPartnerPayableAmount()) < 0) {
            throw new BadRequestException("Insufficient partner pending balance");
        }
        partnerWallet.setPendingBalance(
                partnerWallet.getPendingBalance().subtract(allocation.getPartnerPayableAmount()));
        partnerWallet.setAvailableBalance(
                partnerWallet.getAvailableBalance().add(allocation.getPartnerPayableAmount()));
        partnerWallet.setUpdatedAt(Instant.now());
        walletPortOut.save(partnerWallet);

        FinancialTransaction tx = settlementTransaction(allocationId, FinancialTransactionType.SETTLEMENT_RELEASE);
        postingPolicy.requireBalanced(List.of(
                newEntry(tx, LedgerAccountType.PARTNER_PAYABLE.name(), LedgerEntrySide.DEBIT,
                        allocation.getPartnerPayableAmount()),
                newEntry(tx, LedgerAccountType.PARTNER_PAYABLE.name(), LedgerEntrySide.CREDIT,
                        allocation.getPartnerPayableAmount())));
        // Pending->available is a wallet-balance move; ledger records the release event for audit.
        ledgerPortOut.postBalancedTransaction(tx, List.of(
                newEntry(tx, LedgerAccountType.PARTNER_PAYABLE.name(), LedgerEntrySide.DEBIT,
                        allocation.getPartnerPayableAmount()),
                newEntry(tx, LedgerAccountType.PARTNER_PAYABLE.name(), LedgerEntrySide.CREDIT,
                        allocation.getPartnerPayableAmount())));
        allocation.setSettledAt(Instant.now());
        settlementPortOut.saveAllocation(allocation);
    }

    @Override
    @Transactional
    public PayoutRequest requestPayout(UUID bankAccountId, BigDecimal amount, String idempotencyKey) {
        currentAccountPortIn.requirePermission("WALLET_PAYOUT_CREATE_PARTNER");
        if (bankAccountId == null || amount == null) {
            throw new BadRequestException("bankAccountId and amount must not be null");
        }
        walletPolicy.requireVndWholeUnit(amount);
        String key = TextValidationUtils.normalizeRequiredText(idempotencyKey, "idempotencyKey", 100);
        var existing = settlementPortOut.findPayoutByIdempotency(key);
        if (existing.isPresent()) {
            return existing.get();
        }
        UUID organizationId = walletAccessGuard.resolveCurrentPartnerOrganizationId();
        var bankAccount = settlementPortOut.findBankAccount(bankAccountId)
                .orElseThrow(() -> new NotFoundException("Bank account not found"));
        if (!organizationId.equals(bankAccount.getOrganizationId()) || !bankAccount.isVerified()) {
            throw new BadRequestException("Bank account must be verified and belong to current partner");
        }
        Wallet wallet = walletPortOut.findOrganizationWalletForUpdate(
                organizationId, "VND", WalletPurpose.ORGANIZATION_SETTLEMENT)
                .orElseThrow(() -> new NotFoundException("Partner wallet not found"));
        if (wallet.getAvailableBalance().compareTo(amount) < 0) {
            throw new BadRequestException("Insufficient available balance");
        }
        wallet.setAvailableBalance(wallet.getAvailableBalance().subtract(amount));
        wallet.setHeldBalance(wallet.getHeldBalance().add(amount));
        wallet.setUpdatedAt(Instant.now());
        wallet.setUpdatedBy(currentAccountPortIn.getCurrentAccountIdOrThrow());
        walletPortOut.save(wallet);

        PayoutRequest payout = new PayoutRequest();
        payout.setPayoutRequestId(UUID.randomUUID());
        payout.setWalletId(wallet.getWalletId());
        payout.setOrganizationId(organizationId);
        payout.setBankAccountId(bankAccountId);
        payout.setAmount(amount);
        payout.setCurrency("VND");
        payout.setStatus(PayoutStatus.PENDING);
        payout.setIdempotencyKey(key);
        payout.setRequestedBy(currentAccountPortIn.getCurrentAccountIdOrThrow());
        payout.setRequestedAt(Instant.now());
        try {
            return settlementPortOut.savePayout(payout);
        } catch (DataIntegrityViolationException exception) {
            return settlementPortOut.findPayoutByIdempotency(key)
                    .orElseThrow(() -> new ConflictException("Payout already exists"));
        }
    }

    @Override
    @Transactional
    public PayoutRequest approvePayout(UUID payoutRequestId) {
        currentAccountPortIn.requirePermission("WALLET_PAYOUT_APPROVE_ALL");
        UUID approver = currentAccountPortIn.getCurrentAccountIdOrThrow();
        PayoutRequest payout = settlementPortOut.findPayoutByIdForUpdate(payoutRequestId)
                .orElseThrow(() -> new NotFoundException("Payout not found"));
        if (!PayoutStatus.PENDING.equals(payout.getStatus())) {
            throw new BadRequestException("Only pending payouts can be approved");
        }
        Wallet wallet = walletPortOut.findByIdForUpdate(payout.getWalletId())
                .orElseThrow(() -> new NotFoundException("Wallet not found"));
        if (wallet.getHeldBalance().compareTo(payout.getAmount()) < 0) {
            throw new BadRequestException("Insufficient held balance");
        }
        wallet.setHeldBalance(wallet.getHeldBalance().subtract(payout.getAmount()));
        wallet.setUpdatedAt(Instant.now());
        wallet.setUpdatedBy(approver);
        walletPortOut.save(wallet);

        FinancialTransaction tx = settlementTransaction(payout.getPayoutRequestId(),
                FinancialTransactionType.PAYOUT_COMPLETED);
        LedgerEntry debit = newEntry(tx, LedgerAccountType.PARTNER_PAYABLE.name(), LedgerEntrySide.DEBIT,
                payout.getAmount());
        LedgerEntry credit = newEntry(tx, LedgerAccountType.PLATFORM_CASH_CLEARING.name(),
                LedgerEntrySide.CREDIT, payout.getAmount());
        postingPolicy.requireBalanced(List.of(debit, credit));
        ledgerPortOut.postBalancedTransaction(tx, List.of(debit, credit));

        payout.setStatus(PayoutStatus.COMPLETED);
        payout.setDecidedBy(approver);
        payout.setDecidedAt(Instant.now());
        payout.setFinancialTransactionId(tx.getFinancialTransactionId());
        return settlementPortOut.savePayout(payout);
    }

    @Override
    @Transactional
    public PayoutRequest rejectPayout(UUID payoutRequestId, String reason) {
        boolean isPlatform = currentAccountPortIn.hasPermission("WALLET_PAYOUT_APPROVE_ALL");
        if (!isPlatform) {
            currentAccountPortIn.requirePermission("WALLET_PAYOUT_CREATE_PARTNER");
        }
        UUID actor = currentAccountPortIn.getCurrentAccountIdOrThrow();
        PayoutRequest payout = settlementPortOut.findPayoutByIdForUpdate(payoutRequestId)
                .orElseThrow(() -> new NotFoundException("Payout not found"));
        if (!PayoutStatus.PENDING.equals(payout.getStatus())) {
            throw new BadRequestException("Only pending payouts can be rejected");
        }
        if (actor.equals(payout.getRequestedBy()) && isPlatform) {
            throw new AccessDeniedException("Maker cannot approve own payout");
        }
        Wallet wallet = walletPortOut.findByIdForUpdate(payout.getWalletId())
                .orElseThrow(() -> new NotFoundException("Wallet not found"));
        // Return held -> available.
        wallet.setHeldBalance(wallet.getHeldBalance().subtract(payout.getAmount()));
        wallet.setAvailableBalance(wallet.getAvailableBalance().add(payout.getAmount()));
        wallet.setUpdatedAt(Instant.now());
        walletPortOut.save(wallet);
        payout.setStatus(PayoutStatus.REJECTED);
        payout.setDecidedBy(actor);
        payout.setDecidedAt(Instant.now());
        payout.setFailureReason(reason);
        return settlementPortOut.savePayout(payout);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RevenueAllocation> listMyAllocations(int page, int size) {
        currentAccountPortIn.requirePermission("WALLET_SETTLEMENT_READ_PARTNER");
        UUID organizationId = walletAccessGuard.resolveCurrentPartnerOrganizationId();
        return settlementPortOut.findByOrganization(organizationId, page, size);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PayoutRequest> listMyPayouts(int page, int size) {
        currentAccountPortIn.requirePermission("WALLET_PAYOUT_CREATE_PARTNER");
        UUID organizationId = walletAccessGuard.resolveCurrentPartnerOrganizationId();
        return settlementPortOut.findPayoutsByOrg(organizationId, page, size);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, BigDecimal> financialOverview() {
        currentAccountPortIn.requirePermission("WALLET_RECONCILIATION_READ_ALL");
        Map<String, BigDecimal> overview = new HashMap<>(settlementPortOut.sumLedgerByAccount());
        overview.putIfAbsent("LEDGER_IMBALANCE", ledgerImbalance());
        return overview;
    }

    private UUID resolveOrganizationId(Invoice invoice) {
        if (invoice.getParkingLotId() == null) {
            return null;
        }
        try {
            return organizationPortOut.findOrganizationIdByParkingLotId(invoice.getParkingLotId()).orElse(null);
        } catch (Exception exception) {
            return null;
        }
    }

    private void postAllocationLedger(RevenueAllocation allocation, UUID paymentId) {
        // Example from spec (partner-funded): DEBIT customer liability / CREDIT partner payable, platform revenue, issuer payable.
        FinancialTransaction tx = settlementTransaction(paymentId, FinancialTransactionType.REVENUE_ALLOCATION);
        tx.setIdempotencyKey("ALLOC-" + paymentId);
        tx.setReferenceType("REVENUE_ALLOCATION");
        tx.setReferenceId(paymentId);
        BigDecimal platformRevenue = allocation.getPlatformFeeAmount()
                .subtract(allocation.getVoucherCommissionAmount());
        if (platformRevenue.compareTo(BigDecimal.ZERO) < 0) {
            platformRevenue = BigDecimal.ZERO;
        }
        LedgerEntry debit = newEntry(tx, LedgerAccountType.CUSTOMER_WALLET_LIABILITY.name(),
                LedgerEntrySide.DEBIT, allocation.getCustomerPaidAmount());
        LedgerEntry partnerCredit = newEntry(tx, LedgerAccountType.PARTNER_PAYABLE.name(),
                LedgerEntrySide.CREDIT, allocation.getPartnerPayableAmount());
        LedgerEntry revenueCredit = newEntry(tx, LedgerAccountType.PLATFORM_REVENUE.name(),
                LedgerEntrySide.CREDIT, platformRevenue);
        if (allocation.getVoucherCommissionAmount().compareTo(BigDecimal.ZERO) > 0) {
            LedgerEntry issuerCredit = newEntry(tx, LedgerAccountType.VOUCHER_ISSUER_PAYABLE.name(),
                    LedgerEntrySide.CREDIT, allocation.getVoucherCommissionAmount());
            postingPolicy.requireBalanced(List.of(debit, partnerCredit, revenueCredit, issuerCredit));
            ledgerPortOut.postBalancedTransaction(tx, List.of(debit, partnerCredit, revenueCredit, issuerCredit));
        } else {
            postingPolicy.requireBalanced(List.of(debit, partnerCredit, revenueCredit));
            ledgerPortOut.postBalancedTransaction(tx, List.of(debit, partnerCredit, revenueCredit));
        }
    }

    private FinancialTransaction settlementTransaction(UUID refId, FinancialTransactionType type) {
        FinancialTransaction tx = new FinancialTransaction();
        tx.setFinancialTransactionId(UUID.randomUUID());
        tx.setTransactionCode("STL-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        tx.setTransactionType(type);
        tx.setStatus(FinancialTransactionStatus.POSTED);
        tx.setIdempotencyKey(type.name() + "-" + refId);
        tx.setReferenceType("SETTLEMENT");
        tx.setReferenceId(refId);
        tx.setCurrency("VND");
        tx.setOccurredAt(Instant.now());
        tx.setCreatedAt(Instant.now());
        tx.setCreatedBy(currentAccountPortIn.getCurrentAccountId().orElse(null));
        return tx;
    }

    private LedgerEntry newEntry(FinancialTransaction tx, String accountCode, LedgerEntrySide side, BigDecimal amount) {
        LedgerAccount account = ledgerPortOut.findAccountByCode(accountCode)
                .orElseThrow(() -> new NotFoundException("Ledger account not found: " + accountCode));
        LedgerEntry entry = new LedgerEntry();
        entry.setLedgerEntryId(UUID.randomUUID());
        entry.setFinancialTransactionId(tx.getFinancialTransactionId());
        entry.setLedgerAccountId(account.getLedgerAccountId());
        entry.setEntrySide(side);
        entry.setAmount(amount);
        entry.setDescription("Settlement " + tx.getIdempotencyKey());
        entry.setCreatedAt(Instant.now());
        return entry;
    }

    private BigDecimal ledgerImbalance() {
        try {
            Map<String, BigDecimal> sums = settlementPortOut.sumLedgerByAccount();
            BigDecimal net = BigDecimal.ZERO;
            for (BigDecimal value : sums.values()) {
                if (value != null) {
                    net = net.add(value);
                }
            }
            return net;
        } catch (Exception exception) {
            return BigDecimal.ZERO;
        }
    }
}
