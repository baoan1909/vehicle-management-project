package com.ban.vehicle_management.application.billing.wallet.usecase;

import com.ban.vehicle_management.application.accesscontrol.subscription.port.in.SubscriptionPortIn;
import com.ban.vehicle_management.application.billing.invoice.port.out.InvoicePortOut;
import com.ban.vehicle_management.application.billing.payment.port.out.PaymentPortOut;
import com.ban.vehicle_management.application.billing.wallet.authorization.WalletAccessGuard;
import com.ban.vehicle_management.application.billing.wallet.port.in.WalletPaymentPortIn;
import com.ban.vehicle_management.application.billing.wallet.port.out.LedgerPortOut;
import com.ban.vehicle_management.application.billing.wallet.port.out.WalletPortOut;
import com.ban.vehicle_management.application.iam.account.port.in.CurrentAccountPortIn;
import com.ban.vehicle_management.application.parking.parkingsession.port.in.ParkingCheckoutCompletionPortIn;
import com.ban.vehicle_management.domain.billing.invoice.model.Invoice;
import com.ban.vehicle_management.domain.billing.invoice.policy.InvoicePolicy;
import com.ban.vehicle_management.domain.billing.payment.model.Payment;
import com.ban.vehicle_management.domain.billing.wallet.model.FinancialTransaction;
import com.ban.vehicle_management.domain.billing.wallet.model.LedgerAccount;
import com.ban.vehicle_management.domain.billing.wallet.model.LedgerEntry;
import com.ban.vehicle_management.domain.billing.wallet.model.Wallet;
import com.ban.vehicle_management.domain.billing.wallet.policy.LedgerPostingPolicy;
import com.ban.vehicle_management.domain.billing.wallet.policy.WalletPolicy;
import com.ban.vehicle_management.shared.enumeration.billing.FinancialTransactionStatus;
import com.ban.vehicle_management.shared.enumeration.billing.FinancialTransactionType;
import com.ban.vehicle_management.shared.enumeration.billing.InvoiceStatus;
import com.ban.vehicle_management.shared.enumeration.billing.LedgerAccountType;
import com.ban.vehicle_management.shared.enumeration.billing.LedgerEntrySide;
import com.ban.vehicle_management.shared.enumeration.billing.PaymentMethod;
import com.ban.vehicle_management.shared.enumeration.billing.PaymentStatus;
import com.ban.vehicle_management.shared.enumeration.billing.WalletPurpose;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import com.ban.vehicle_management.shared.exception.ConflictException;
import com.ban.vehicle_management.shared.exception.NotFoundException;
import com.ban.vehicle_management.shared.utils.TextValidationUtils;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Phase 3: pay invoices with customer wallet; refund via reversal ledger entries.
 * Amount always comes from invoice.finalAmount; client amount is never trusted.
 * Fixed lock order: wallet -&gt; invoice -&gt; payment to avoid deadlocks.
 */
@Service
public class WalletInvoicePaymentUseCaseImpl implements WalletPaymentPortIn {

    private static final String CURRENCY = "VND";

    private final WalletPortOut walletPortOut;
    private final InvoicePortOut invoicePortOut;
    private final PaymentPortOut paymentPortOut;
    private final LedgerPortOut ledgerPortOut;
    private final WalletAccessGuard walletAccessGuard;
    private final CurrentAccountPortIn currentAccountPortIn;
    private final SubscriptionPortIn subscriptionPortIn;
    private final ParkingCheckoutCompletionPortIn parkingCheckoutCompletionPortIn;
    private final WalletPolicy walletPolicy = new WalletPolicy();
    private final InvoicePolicy invoicePolicy = new InvoicePolicy();
    private final LedgerPostingPolicy postingPolicy = new LedgerPostingPolicy();

    public WalletInvoicePaymentUseCaseImpl(
            WalletPortOut walletPortOut,
            InvoicePortOut invoicePortOut,
            PaymentPortOut paymentPortOut,
            LedgerPortOut ledgerPortOut,
            WalletAccessGuard walletAccessGuard,
            CurrentAccountPortIn currentAccountPortIn,
            SubscriptionPortIn subscriptionPortIn,
            ParkingCheckoutCompletionPortIn parkingCheckoutCompletionPortIn) {
        this.walletPortOut = walletPortOut;
        this.invoicePortOut = invoicePortOut;
        this.paymentPortOut = paymentPortOut;
        this.ledgerPortOut = ledgerPortOut;
        this.walletAccessGuard = walletAccessGuard;
        this.currentAccountPortIn = currentAccountPortIn;
        this.subscriptionPortIn = subscriptionPortIn;
        this.parkingCheckoutCompletionPortIn = parkingCheckoutCompletionPortIn;
    }

    @Override
    @Transactional
    public Payment payInvoice(UUID invoiceId, String idempotencyKey) {
        currentAccountPortIn.requirePermission("WALLET_PAY_INVOICE_OWN");
        if (invoiceId == null) {
            throw new BadRequestException("invoiceId must not be null");
        }
        String normalizedKey = TextValidationUtils.normalizeRequiredText(idempotencyKey, "idempotencyKey", 100);

        var existingPayment = paymentPortOut.findByIdempotencyKey(normalizedKey);
        if (existingPayment.isPresent()) {
            return existingPayment.get();
        }

        UUID customerId = walletAccessGuard.resolveCurrentApprovedCustomerId();
        // Fixed lock order: wallet first, then invoice.
        Wallet wallet = walletPortOut.findCustomerWalletForUpdate(customerId, CURRENCY, WalletPurpose.PERSONAL)
                .orElseThrow(() -> new NotFoundException("Wallet not found"));
        walletAccessGuard.ensureCanReadCustomerWallet(wallet);
        Invoice invoice = invoicePortOut.findByIdForUpdate(invoiceId)
                .orElseThrow(() -> new NotFoundException("Invoice not found"));
        if (!customerId.equals(invoice.getCustomerId())) {
            throw new AccessDeniedException("Access is denied");
        }
        if (!InvoiceStatus.UNPAID.equals(invoice.getStatus())) {
            throw new ConflictException("Only unpaid invoice can be paid");
        }
        if (invoice.getFinalAmount() == null || invoice.getFinalAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Invoice amount must be greater than zero");
        }
        if (paymentPortOut.existsByInvoiceIdAndStatus(invoiceId, PaymentStatus.SUCCESS)) {
            throw new ConflictException("Successful payment already exists for this invoice");
        }
        walletPolicy.requireCanDebit(wallet);
        walletPolicy.requireVndWholeUnit(invoice.getFinalAmount());
        walletPolicy.requireSufficientAvailable(wallet, invoice.getFinalAmount());

        wallet.setAvailableBalance(wallet.getAvailableBalance().subtract(invoice.getFinalAmount()));
        wallet.setUpdatedAt(Instant.now());
        wallet.setUpdatedBy(currentAccountPortIn.getCurrentAccountIdOrThrow());
        walletPortOut.save(wallet);

        FinancialTransaction transaction = newTransaction(
                FinancialTransactionType.INVOICE_PAYMENT, normalizedKey, invoiceId);
        LedgerAccount liability = requireAccount(LedgerAccountType.CUSTOMER_WALLET_LIABILITY.name());
        LedgerAccount clearing = requireAccount(LedgerAccountType.PLATFORM_CASH_CLEARING.name());
        LedgerEntry debit = newEntry(transaction, liability, LedgerEntrySide.DEBIT, invoice.getFinalAmount(),
                "Wallet payment invoice " + invoice.getInvoiceNo());
        LedgerEntry credit = newEntry(transaction, clearing, LedgerEntrySide.CREDIT, invoice.getFinalAmount(),
                "Wallet payment invoice " + invoice.getInvoiceNo());
        postingPolicy.requireBalanced(List.of(debit, credit));
        try {
            ledgerPortOut.postBalancedTransaction(transaction, List.of(debit, credit));
        } catch (Exception exception) {
            throw new ConflictException("Payment already processed");
        }

        Payment payment = new Payment();
        payment.setPaymentId(UUID.randomUUID());
        payment.setInvoiceId(invoiceId);
        payment.setPaymentMethod(PaymentMethod.WALLET);
        payment.setAmount(invoice.getFinalAmount());
        payment.setTransactionRef("WLP-" + transaction.getTransactionCode());
        payment.setStatus(PaymentStatus.SUCCESS);
        payment.setPaidAt(Instant.now());
        payment.setWalletId(wallet.getWalletId());
        payment.setIdempotencyKey(normalizedKey);
        payment.setRefundedAmount(BigDecimal.ZERO);
        payment.setCreatedAt(Instant.now());
        payment.setUpdatedAt(Instant.now());
        Payment saved;
        try {
            saved = paymentPortOut.save(payment);
        } catch (DataIntegrityViolationException exception) {
            return paymentPortOut.findByIdempotencyKey(normalizedKey)
                    .orElseThrow(() -> new ConflictException("Payment already exists"));
        }

        invoicePolicy.markPaid(invoice, saved.getPaidAt());
        invoicePortOut.save(invoice);
        if (invoice.getSubscriptionId() != null) {
            subscriptionPortIn.markSubscriptionPaymentCompleted(invoice.getSubscriptionId());
        }
        parkingCheckoutCompletionPortIn.completePaidCheckout(invoice.getInvoiceId());
        return saved;
    }

    @Override
    @Transactional
    public Payment refundPayment(UUID paymentId, BigDecimal amount, String idempotencyKey, String reason) {
        boolean canRefundAll = currentAccountPortIn.hasPermission("WALLET_REFUND_ALL");
        if (!canRefundAll) {
            currentAccountPortIn.requirePermission("WALLET_REFUND_READ_OWN");
        }
        if (paymentId == null) {
            throw new BadRequestException("paymentId must not be null");
        }
        String normalizedKey = TextValidationUtils.normalizeRequiredText(idempotencyKey, "idempotencyKey", 100);
        var existing = paymentPortOut.findByIdempotencyKey(normalizedKey);
        if (existing.isPresent()) {
            return existing.get();
        }

        Payment original = paymentPortOut.findByIdForUpdate(paymentId)
                .orElseThrow(() -> new NotFoundException("Payment not found"));
        if (!PaymentStatus.SUCCESS.equals(original.getStatus())) {
            throw new ConflictException("Only successful payment can be refunded");
        }
        if (!PaymentMethod.WALLET.equals(original.getPaymentMethod())) {
            throw new BadRequestException("Only wallet payments can be refunded via wallet");
        }
        BigDecimal refundAmount = amount == null ? original.getAmount() : amount;
        walletPolicy.requireVndWholeUnit(refundAmount);
        BigDecimal alreadyRefunded = original.getRefundedAmount() == null ? BigDecimal.ZERO : original.getRefundedAmount();
        if (alreadyRefunded.add(refundAmount).compareTo(original.getAmount()) > 0) {
            throw new BadRequestException("Refund amount exceeds original payment");
        }

        Invoice invoice = invoicePortOut.findByIdForUpdate(original.getInvoiceId())
                .orElseThrow(() -> new NotFoundException("Invoice not found"));
        if (!canRefundAll) {
            UUID customerId = walletAccessGuard.resolveCurrentApprovedCustomerId();
            if (!customerId.equals(invoice.getCustomerId())) {
                throw new AccessDeniedException("Access is denied");
            }
        }
        Wallet wallet = walletPortOut.findByIdForUpdate(original.getWalletId())
                .orElseThrow(() -> new NotFoundException("Wallet not found"));
        walletPolicy.requireCanCredit(wallet);
        wallet.setAvailableBalance(wallet.getAvailableBalance().add(refundAmount));
        wallet.setUpdatedAt(Instant.now());
        walletPortOut.save(wallet);

        FinancialTransaction reversal = newTransaction(FinancialTransactionType.REFUND, normalizedKey, paymentId);
        LedgerAccount liability = requireAccount(LedgerAccountType.CUSTOMER_WALLET_LIABILITY.name());
        LedgerAccount clearing = requireAccount(LedgerAccountType.PLATFORM_CASH_CLEARING.name());
        LedgerEntry debit = newEntry(reversal, clearing, LedgerEntrySide.DEBIT, refundAmount,
                "Wallet refund payment " + original.getPaymentId());
        LedgerEntry credit = newEntry(reversal, liability, LedgerEntrySide.CREDIT, refundAmount,
                "Wallet refund payment " + original.getPaymentId());
        postingPolicy.requireBalanced(List.of(debit, credit));
        ledgerPortOut.postBalancedTransaction(reversal, List.of(debit, credit));

        original.setRefundedAmount(alreadyRefunded.add(refundAmount));
        if (original.getRefundedAmount().compareTo(original.getAmount()) == 0) {
            original.setStatus(PaymentStatus.REFUNDED);
        }
        original.setUpdatedAt(Instant.now());
        paymentPortOut.save(original);

        Payment refundRecord = new Payment();
        refundRecord.setPaymentId(UUID.randomUUID());
        refundRecord.setInvoiceId(original.getInvoiceId());
        refundRecord.setPaymentMethod(PaymentMethod.WALLET);
        refundRecord.setAmount(refundAmount.negate());
        refundRecord.setTransactionRef("WLR-" + reversal.getTransactionCode());
        refundRecord.setStatus(PaymentStatus.REFUNDED);
        refundRecord.setPaidAt(Instant.now());
        refundRecord.setWalletId(wallet.getWalletId());
        refundRecord.setIdempotencyKey(normalizedKey);
        refundRecord.setReversedPaymentId(original.getPaymentId());
        refundRecord.setRefundedAmount(BigDecimal.ZERO);
        refundRecord.setCreatedAt(Instant.now());
        refundRecord.setUpdatedAt(Instant.now());
        try {
            return paymentPortOut.save(refundRecord);
        } catch (DataIntegrityViolationException exception) {
            return paymentPortOut.findByIdempotencyKey(normalizedKey)
                    .orElseThrow(() -> new ConflictException("Refund already exists"));
        }
    }

    private FinancialTransaction newTransaction(FinancialTransactionType type, String idemKey, UUID refId) {
        FinancialTransaction transaction = new FinancialTransaction();
        transaction.setFinancialTransactionId(UUID.randomUUID());
        transaction.setTransactionCode("WLT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        transaction.setTransactionType(type);
        transaction.setStatus(FinancialTransactionStatus.POSTED);
        transaction.setIdempotencyKey("WALLETPAY-" + idemKey);
        transaction.setReferenceType("WALLET_PAYMENT");
        transaction.setReferenceId(refId);
        transaction.setCurrency(CURRENCY);
        transaction.setOccurredAt(Instant.now());
        transaction.setCreatedAt(Instant.now());
        transaction.setCreatedBy(currentAccountPortIn.getCurrentAccountId().orElse(null));
        return transaction;
    }

    private LedgerAccount requireAccount(String code) {
        return ledgerPortOut.findAccountByCode(code)
                .orElseThrow(() -> new NotFoundException("Ledger account not found: " + code));
    }

    private LedgerEntry newEntry(FinancialTransaction tx, LedgerAccount account, LedgerEntrySide side,
            BigDecimal amount, String description) {
        LedgerEntry entry = new LedgerEntry();
        entry.setLedgerEntryId(UUID.randomUUID());
        entry.setFinancialTransactionId(tx.getFinancialTransactionId());
        entry.setLedgerAccountId(account.getLedgerAccountId());
        entry.setEntrySide(side);
        entry.setAmount(amount);
        entry.setDescription(description);
        entry.setCreatedAt(Instant.now());
        return entry;
    }
}
