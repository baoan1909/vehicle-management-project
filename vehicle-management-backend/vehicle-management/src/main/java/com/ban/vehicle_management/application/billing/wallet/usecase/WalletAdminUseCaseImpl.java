package com.ban.vehicle_management.application.billing.wallet.usecase;

import com.ban.vehicle_management.application.billing.wallet.authorization.WalletAccessGuard;
import com.ban.vehicle_management.application.billing.wallet.port.in.WalletAdminPortIn;
import com.ban.vehicle_management.application.billing.wallet.port.out.LedgerPortOut;
import com.ban.vehicle_management.application.billing.wallet.port.out.WalletAdjustmentPortOut;
import com.ban.vehicle_management.application.billing.wallet.port.out.WalletPortOut;
import com.ban.vehicle_management.application.iam.account.port.in.CurrentAccountPortIn;
import com.ban.vehicle_management.domain.billing.wallet.model.FinancialTransaction;
import com.ban.vehicle_management.domain.billing.wallet.model.LedgerAccount;
import com.ban.vehicle_management.domain.billing.wallet.model.LedgerEntry;
import com.ban.vehicle_management.domain.billing.wallet.model.Wallet;
import com.ban.vehicle_management.domain.billing.wallet.model.WalletAdjustment;
import com.ban.vehicle_management.domain.billing.wallet.policy.LedgerPostingPolicy;
import com.ban.vehicle_management.domain.billing.wallet.policy.WalletPolicy;
import com.ban.vehicle_management.shared.enumeration.billing.FinancialTransactionStatus;
import com.ban.vehicle_management.shared.enumeration.billing.FinancialTransactionType;
import com.ban.vehicle_management.shared.enumeration.billing.LedgerAccountType;
import com.ban.vehicle_management.shared.enumeration.billing.LedgerEntrySide;
import com.ban.vehicle_management.shared.enumeration.billing.WalletAdjustmentStatus;
import com.ban.vehicle_management.shared.enumeration.billing.WalletOwnerType;
import com.ban.vehicle_management.shared.enumeration.billing.WalletStatus;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import com.ban.vehicle_management.shared.exception.NotFoundException;
import com.ban.vehicle_management.shared.utils.TextValidationUtils;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WalletAdminUseCaseImpl implements WalletAdminPortIn {

    private final WalletPortOut walletPortOut;
    private final LedgerPortOut ledgerPortOut;
    private final WalletAdjustmentPortOut adjustmentPortOut;
    private final CurrentAccountPortIn currentAccountPortIn;
    private final WalletPolicy walletPolicy = new WalletPolicy();
    private final LedgerPostingPolicy postingPolicy = new LedgerPostingPolicy();

    public WalletAdminUseCaseImpl(
            WalletPortOut walletPortOut,
            LedgerPortOut ledgerPortOut,
            WalletAdjustmentPortOut adjustmentPortOut,
            CurrentAccountPortIn currentAccountPortIn) {
        this.walletPortOut = walletPortOut;
        this.ledgerPortOut = ledgerPortOut;
        this.adjustmentPortOut = adjustmentPortOut;
        this.currentAccountPortIn = currentAccountPortIn;
    }

    @Override
    @Transactional
    public void lockWallet(UUID walletId) {
        currentAccountPortIn.requirePermission(WalletAccessGuard.LOCK_ALL);
        Wallet wallet = walletPortOut.findByIdForUpdate(walletId)
                .orElseThrow(() -> new NotFoundException("Wallet not found"));
        wallet.setStatus(WalletStatus.LOCKED);
        wallet.setUpdatedAt(Instant.now());
        wallet.setUpdatedBy(currentAccountPortIn.getCurrentAccountIdOrThrow());
        walletPortOut.save(wallet);
    }

    @Override
    @Transactional
    public void unlockWallet(UUID walletId) {
        currentAccountPortIn.requirePermission(WalletAccessGuard.UNLOCK_ALL);
        Wallet wallet = walletPortOut.findByIdForUpdate(walletId)
                .orElseThrow(() -> new NotFoundException("Wallet not found"));
        if (WalletStatus.CLOSED.equals(wallet.getStatus())) {
            throw new BadRequestException("Closed wallet cannot be unlocked");
        }
        wallet.setStatus(WalletStatus.ACTIVE);
        wallet.setUpdatedAt(Instant.now());
        wallet.setUpdatedBy(currentAccountPortIn.getCurrentAccountIdOrThrow());
        walletPortOut.save(wallet);
    }

    @Override
    @Transactional
    public WalletAdjustment requestAdjustment(
            UUID walletId, BigDecimal amount, String direction, String reason) {
        currentAccountPortIn.requirePermission(WalletAccessGuard.ADJUST_REQUEST_ALL);
        Wallet wallet = walletPortOut.findById(walletId)
                .orElseThrow(() -> new NotFoundException("Wallet not found"));
        walletPolicy.requireVndWholeUnit(amount);
        String normalizedDirection = normalizeDirection(direction);
        String normalizedReason = TextValidationUtils.normalizeRequiredText(reason, "reason", 500);
        WalletAdjustment adjustment = new WalletAdjustment();
        adjustment.setWalletAdjustmentId(UUID.randomUUID());
        adjustment.setWalletId(wallet.getWalletId());
        adjustment.setAmount(amount);
        adjustment.setDirection(normalizedDirection);
        adjustment.setReason(normalizedReason);
        adjustment.setStatus(WalletAdjustmentStatus.PENDING);
        adjustment.setRequestedBy(currentAccountPortIn.getCurrentAccountIdOrThrow());
        adjustment.setRequestedAt(Instant.now());
        return adjustmentPortOut.save(adjustment);
    }

    @Override
    @Transactional
    public WalletAdjustment approveAdjustment(UUID adjustmentId) {
        currentAccountPortIn.requirePermission(WalletAccessGuard.ADJUST_APPROVE_ALL);
        UUID approverId = currentAccountPortIn.getCurrentAccountIdOrThrow();
        WalletAdjustment adjustment = adjustmentPortOut.findByIdForUpdate(adjustmentId)
                .orElseThrow(() -> new NotFoundException("Adjustment not found"));
        if (!WalletAdjustmentStatus.PENDING.equals(adjustment.getStatus())) {
            throw new BadRequestException("Only pending adjustments can be approved");
        }
        if (approverId.equals(adjustment.getRequestedBy())) {
            throw new AccessDeniedException("Maker cannot approve own adjustment");
        }
        Wallet wallet = walletPortOut.findByIdForUpdate(adjustment.getWalletId())
                .orElseThrow(() -> new NotFoundException("Wallet not found"));

        boolean credit = "CREDIT".equals(adjustment.getDirection());
        if (credit) {
            walletPolicy.requireCanCredit(wallet);
            wallet.setAvailableBalance(wallet.getAvailableBalance().add(adjustment.getAmount()));
        } else {
            walletPolicy.requireCanDebit(wallet);
            walletPolicy.requireSufficientAvailable(wallet, adjustment.getAmount());
            wallet.setAvailableBalance(wallet.getAvailableBalance().subtract(adjustment.getAmount()));
        }
        wallet.setUpdatedAt(Instant.now());
        wallet.setUpdatedBy(approverId);

        FinancialTransaction transaction = newTransaction(
                FinancialTransactionType.WALLET_ADJUSTMENT, approverId, adjustment.getWalletAdjustmentId());
        List<LedgerEntry> entries = balancedAdjustmentEntries(wallet, adjustment, transaction);
        postingPolicy.requireBalanced(entries);
        ledgerPortOut.postBalancedTransaction(transaction, entries);

        walletPortOut.save(wallet);
        adjustment.setStatus(WalletAdjustmentStatus.APPROVED);
        adjustment.setDecidedBy(approverId);
        adjustment.setDecidedAt(Instant.now());
        adjustment.setFinancialTransactionId(transaction.getFinancialTransactionId());
        return adjustmentPortOut.save(adjustment);
    }

    @Override
    @Transactional
    public WalletAdjustment rejectAdjustment(UUID adjustmentId, String reason) {
        currentAccountPortIn.requirePermission(WalletAccessGuard.ADJUST_APPROVE_ALL);
        UUID approverId = currentAccountPortIn.getCurrentAccountIdOrThrow();
        WalletAdjustment adjustment = adjustmentPortOut.findByIdForUpdate(adjustmentId)
                .orElseThrow(() -> new NotFoundException("Adjustment not found"));
        if (!WalletAdjustmentStatus.PENDING.equals(adjustment.getStatus())) {
            throw new BadRequestException("Only pending adjustments can be rejected");
        }
        if (approverId.equals(adjustment.getRequestedBy())) {
            throw new AccessDeniedException("Maker cannot reject own adjustment");
        }
        adjustment.setStatus(WalletAdjustmentStatus.REJECTED);
        adjustment.setDecidedBy(approverId);
        adjustment.setDecidedAt(Instant.now());
        if (reason != null && !reason.isBlank()) {
            adjustment.setReason(TextValidationUtils.normalizeNullableText(
                    adjustment.getReason() + " | reject: " + reason.trim(), "reason", 500));
        }
        return adjustmentPortOut.save(adjustment);
    }

    @Override
    @Transactional(readOnly = true)
    public List<WalletAdjustment> listAdjustments(UUID walletId) {
        currentAccountPortIn.requirePermission(WalletAccessGuard.READ_ALL);
        walletPortOut.findById(walletId).orElseThrow(() -> new NotFoundException("Wallet not found"));
        return adjustmentPortOut.findByWallet(walletId);
    }

    private String normalizeDirection(String direction) {
        if (direction == null) {
            throw new BadRequestException("direction must not be null");
        }
        String normalized = direction.trim().toUpperCase();
        if (!"CREDIT".equals(normalized) && !"DEBIT".equals(normalized)) {
            throw new BadRequestException("direction must be CREDIT or DEBIT");
        }
        return normalized;
    }

    private FinancialTransaction newTransaction(
            FinancialTransactionType type, UUID actorId, UUID referenceId) {
        FinancialTransaction transaction = new FinancialTransaction();
        transaction.setFinancialTransactionId(UUID.randomUUID());
        transaction.setTransactionCode("WLT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        transaction.setTransactionType(type);
        transaction.setStatus(FinancialTransactionStatus.POSTED);
        transaction.setIdempotencyKey("ADJ-" + referenceId);
        transaction.setReferenceType("WALLET");
        transaction.setReferenceId(referenceId);
        transaction.setCurrency("VND");
        transaction.setOccurredAt(Instant.now());
        transaction.setCreatedAt(Instant.now());
        transaction.setCreatedBy(actorId);
        return transaction;
    }

    private List<LedgerEntry> balancedAdjustmentEntries(
            Wallet wallet, WalletAdjustment adjustment, FinancialTransaction transaction) {
        LedgerAccountType walletLeg = WalletOwnerType.ORGANIZATION.equals(wallet.getOwnerType())
                ? LedgerAccountType.PARTNER_PAYABLE
                : LedgerAccountType.CUSTOMER_WALLET_LIABILITY;
        LedgerAccount debitAccount;
        LedgerAccount creditAccount;
        if ("CREDIT".equals(adjustment.getDirection())) {
            debitAccount = requireAccount(LedgerAccountType.PLATFORM_CASH_CLEARING.name());
            creditAccount = requireAccount(walletLeg.name());
        } else {
            debitAccount = requireAccount(walletLeg.name());
            creditAccount = requireAccount(LedgerAccountType.PLATFORM_CASH_CLEARING.name());
        }
        LedgerEntry debit = newEntry(transaction, debitAccount, LedgerEntrySide.DEBIT, adjustment.getAmount());
        LedgerEntry credit = newEntry(transaction, creditAccount, LedgerEntrySide.CREDIT, adjustment.getAmount());
        return List.of(debit, credit);
    }

    private LedgerAccount requireAccount(String accountCode) {
        return ledgerPortOut.findAccountByCode(accountCode)
                .orElseThrow(() -> new NotFoundException("Ledger account not found: " + accountCode));
    }

    private LedgerEntry newEntry(
            FinancialTransaction transaction, LedgerAccount account, LedgerEntrySide side, BigDecimal amount) {
        LedgerEntry entry = new LedgerEntry();
        entry.setLedgerEntryId(UUID.randomUUID());
        entry.setFinancialTransactionId(transaction.getFinancialTransactionId());
        entry.setLedgerAccountId(account.getLedgerAccountId());
        entry.setEntrySide(side);
        entry.setAmount(amount);
        entry.setDescription("Wallet adjustment " + transaction.getIdempotencyKey());
        entry.setCreatedAt(Instant.now());
        return entry;
    }
}
