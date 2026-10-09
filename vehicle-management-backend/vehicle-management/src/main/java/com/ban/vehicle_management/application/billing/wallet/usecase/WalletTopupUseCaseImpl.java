package com.ban.vehicle_management.application.billing.wallet.usecase;

import com.ban.vehicle_management.application.billing.payment.model.VnpayPaymentRequest;
import com.ban.vehicle_management.application.billing.payment.model.command.VnpayCallbackCommand;
import com.ban.vehicle_management.application.billing.payment.model.result.VnpayCallbackData;
import com.ban.vehicle_management.application.billing.payment.model.result.VnpayIpnResult;
import com.ban.vehicle_management.application.billing.payment.model.result.VnpayPaymentLink;
import com.ban.vehicle_management.application.billing.payment.model.result.VnpayReturnResult;
import com.ban.vehicle_management.application.billing.payment.port.out.VnpayGatewayPortOut;
import com.ban.vehicle_management.application.billing.wallet.authorization.WalletAccessGuard;
import com.ban.vehicle_management.application.billing.wallet.port.in.WalletTopupPortIn;
import com.ban.vehicle_management.application.billing.wallet.port.out.LedgerPortOut;
import com.ban.vehicle_management.application.billing.wallet.port.out.WalletPortOut;
import com.ban.vehicle_management.application.billing.wallet.port.out.WalletTopupPortOut;
import com.ban.vehicle_management.application.iam.account.port.in.CurrentAccountPortIn;
import com.ban.vehicle_management.application.notification.notification.model.SendNotificationCommand;
import com.ban.vehicle_management.application.notification.notification.port.in.NotificationPortIn;
import com.ban.vehicle_management.domain.billing.wallet.model.FinancialTransaction;
import com.ban.vehicle_management.domain.billing.wallet.model.LedgerAccount;
import com.ban.vehicle_management.domain.billing.wallet.model.LedgerEntry;
import com.ban.vehicle_management.domain.billing.wallet.model.Wallet;
import com.ban.vehicle_management.domain.billing.wallet.model.WalletTopupOrder;
import com.ban.vehicle_management.domain.billing.wallet.policy.LedgerPostingPolicy;
import com.ban.vehicle_management.domain.billing.wallet.policy.WalletPolicy;
import com.ban.vehicle_management.domain.billing.wallet.policy.WalletTopupPolicy;
import com.ban.vehicle_management.shared.enumeration.billing.FinancialTransactionStatus;
import com.ban.vehicle_management.shared.enumeration.billing.FinancialTransactionType;
import com.ban.vehicle_management.shared.enumeration.billing.LedgerAccountType;
import com.ban.vehicle_management.shared.enumeration.billing.LedgerEntrySide;
import com.ban.vehicle_management.shared.enumeration.billing.WalletPurpose;
import com.ban.vehicle_management.shared.enumeration.billing.WalletStatus;
import com.ban.vehicle_management.shared.enumeration.billing.WalletTopupStatus;
import com.ban.vehicle_management.shared.enumeration.notification.NotificationType;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import com.ban.vehicle_management.shared.exception.NotFoundException;
import com.ban.vehicle_management.shared.utils.TextValidationUtils;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

/**
 * Phase 2: Customer wallet top-up via VNPAY.
 * IPN / server callback is the source of truth; return URL never mutates money on its own.
 */
@Service
public class WalletTopupUseCaseImpl implements WalletTopupPortIn {

    private static final String CURRENCY = "VND";
    private static final String TOP_UP_PERMISSION = "WALLET_TOP_UP_OWN";

    private final WalletPortOut walletPortOut;
    private final WalletTopupPortOut topupPortOut;
    private final LedgerPortOut ledgerPortOut;
    private final VnpayGatewayPortOut vnpayGatewayPortOut;
    private final WalletAccessGuard walletAccessGuard;
    private final CurrentAccountPortIn currentAccountPortIn;
    private final NotificationPortIn notificationPortIn;
    private final String walletReturnUrl;
    private final WalletPolicy walletPolicy = new WalletPolicy();
    private final WalletTopupPolicy topupPolicy = new WalletTopupPolicy();
    private final LedgerPostingPolicy postingPolicy = new LedgerPostingPolicy();

    public WalletTopupUseCaseImpl(
            WalletPortOut walletPortOut,
            WalletTopupPortOut topupPortOut,
            LedgerPortOut ledgerPortOut,
            VnpayGatewayPortOut vnpayGatewayPortOut,
            WalletAccessGuard walletAccessGuard,
            CurrentAccountPortIn currentAccountPortIn,
            NotificationPortIn notificationPortIn,
            @org.springframework.beans.factory.annotation.Value("${app.wallet.vnpay.return-url:}") String walletReturnUrl) {
        this.walletPortOut = walletPortOut;
        this.topupPortOut = topupPortOut;
        this.ledgerPortOut = ledgerPortOut;
        this.vnpayGatewayPortOut = vnpayGatewayPortOut;
        this.walletAccessGuard = walletAccessGuard;
        this.currentAccountPortIn = currentAccountPortIn;
        this.notificationPortIn = notificationPortIn;
        this.walletReturnUrl = walletReturnUrl;
    }

    @Override
    @Transactional
    public WalletTopupOrder createTopup(BigDecimal amount, String idempotencyKey, String clientIp) {
        currentAccountPortIn.requirePermission(TOP_UP_PERMISSION);
        topupPolicy.requireValidAmount(amount);
        String normalizedKey = TextValidationUtils.normalizeRequiredText(idempotencyKey, "idempotencyKey", 100);

        topupPortOut.lockIdempotencyKey(normalizedKey);
        var existingByKey = topupPortOut.findByIdempotencyKey(normalizedKey);
        if (existingByKey.isPresent()) {
            return existingByKey.get();
        }

        UUID customerId = walletAccessGuard.resolveCurrentApprovedCustomerId();
        Wallet wallet = walletPortOut
                .findCustomerWallet(customerId, CURRENCY, WalletPurpose.PERSONAL)
                .orElseGet(() -> provisionWallet(customerId));
        if (!WalletStatus.ACTIVE.equals(wallet.getStatus())) {
            throw new BadRequestException("Wallet is not active and cannot be topped up");
        }

        UUID orderId = UUID.randomUUID();
        String transactionRef = "WLT" + orderId.toString().replace("-", "").substring(0, 24).toUpperCase();
        Instant now = Instant.now();
        VnpayPaymentLink link = vnpayGatewayPortOut.createPaymentLink(new VnpayPaymentRequest(
                transactionRef, amount, "Nap tien vi CoParking " + transactionRef,
                clientIp == null ? "127.0.0.1" : clientIp, null, "vn", now,
                walletReturnUrl == null || walletReturnUrl.isBlank() ? null : walletReturnUrl.trim()));

        WalletTopupOrder order = new WalletTopupOrder();
        order.setTopupOrderId(orderId);
        order.setWalletId(wallet.getWalletId());
        order.setAmount(amount);
        order.setCurrency(CURRENCY);
        order.setStatus(WalletTopupStatus.PENDING);
        order.setTransactionRef(transactionRef);
        order.setPaymentUrl(link.paymentUrl());
        order.setExpiresAt(link.expiresAt());
        order.setIdempotencyKey(normalizedKey);
        order.setCreatedAt(now);
        order.setCreatedBy(currentAccountPortIn.getCurrentAccountId().orElse(null));
        return topupPortOut.save(order);
    }

    @Override
    @Transactional(readOnly = true)
    public WalletTopupOrder getTopup(UUID topupOrderId) {
        currentAccountPortIn.requirePermission(TOP_UP_PERMISSION);
        WalletTopupOrder order = topupPortOut.findById(topupOrderId)
                .orElseThrow(() -> new NotFoundException("Top-up order not found"));
        ensureOwnOrder(order);
        return order;
    }

    @Override
    @Transactional(readOnly = true)
    public List<WalletTopupOrder> listMyTopups(int page, int size) {
        currentAccountPortIn.requirePermission(TOP_UP_PERMISSION);
        UUID customerId = walletAccessGuard.resolveCurrentApprovedCustomerId();
        Wallet wallet = walletPortOut.findCustomerWallet(customerId, CURRENCY, WalletPurpose.PERSONAL)
                .orElseThrow(() -> new NotFoundException("Wallet not found"));
        return topupPortOut.findByWallet(wallet.getWalletId(), page, size);
    }

    @Override
    @Transactional
    public VnpayIpnResult processIpn(VnpayCallbackCommand command) {
        VnpayCallbackData callback = verifyCallback(command);
        if (!callback.validSignature()) {
            return new VnpayIpnResult("97", "Invalid signature");
        }
        if (!StringUtils.hasText(callback.transactionRef())) {
            return new VnpayIpnResult("01", "Order not found");
        }
        WalletTopupOrder order = topupPortOut.findByTransactionRefForUpdate(callback.transactionRef())
                .orElse(null);
        if (order == null) {
            return new VnpayIpnResult("01", "Order not found");
        }
        if (callback.amount() == null || order.getAmount().compareTo(callback.amount()) != 0) {
            return new VnpayIpnResult("04", "Invalid amount");
        }
        if (!WalletTopupStatus.PENDING.equals(order.getStatus())) {
            return new VnpayIpnResult("02", "Order already confirmed");
        }
        if (order.getExpiresAt() != null && !order.getExpiresAt().isAfter(Instant.now())) {
            order.setStatus(WalletTopupStatus.EXPIRED);
            order.setFailureReason("EXPIRED");
            topupPortOut.save(order);
            return new VnpayIpnResult("02", "Order already confirmed");
        }

        if (callback.isSuccessful()) {
            completeTopup(order, callback);
        } else {
            order.setStatus(WalletTopupStatus.FAILED);
            order.setProviderTransactionNo(callback.providerTransactionNo());
            order.setProviderResponseCode(callback.responseCode());
            order.setProviderTransactionStatus(callback.transactionStatus());
            order.setFailureReason(callback.responseCode() + "/" + callback.transactionStatus());
            topupPortOut.save(order);
        }
        return new VnpayIpnResult("00", "Confirm Success");
    }

    @Override
    @Transactional(readOnly = true)
    public VnpayReturnResult verifyReturn(VnpayCallbackCommand command) {
        VnpayCallbackData callback = verifyCallback(command);
        WalletTopupOrder order = StringUtils.hasText(callback.transactionRef())
                ? topupPortOut.findByTransactionRef(callback.transactionRef()).orElse(null)
                : null;
        // Return URL is display-only: never mutate balances here.
        return new VnpayReturnResult(
                callback.validSignature(), callback.isSuccessful(), callback.transactionRef(),
                callback.responseCode(), callback.transactionStatus(),
                order == null ? null : mapToPaymentStatus(order.getStatus()));
    }

    @Override
    @Transactional
    public int expireOverdueTopups(int batchSize) {
        List<UUID> claimed = topupPortOut.claimExpiredPending(Math.max(1, Math.min(batchSize, 500)), Instant.now());
        return claimed.size();
    }

    @Override
    @Transactional(readOnly = true)
    public List<WalletTopupOrder> findReconciliationExceptions(int page, int size) {
        currentAccountPortIn.requirePermission("WALLET_RECONCILIATION_READ_ALL");
        return topupPortOut.findReconciliationExceptions(page, size);
    }

    private void completeTopup(WalletTopupOrder order, VnpayCallbackData callback) {
        Wallet wallet = walletPortOut.findByIdForUpdate(order.getWalletId())
                .orElseThrow(() -> new NotFoundException("Wallet not found"));
        walletPolicy.requireCanCredit(wallet);
        walletPolicy.requireVndWholeUnit(order.getAmount());

        // Idempotency: same transaction ref posts exactly one ledger transaction.
        String txKey = "TOPUP-" + order.getTopupOrderId();
        if (ledgerPortOut.findTransactionByIdempotencyKey(txKey).isPresent()) {
            order.setStatus(WalletTopupStatus.COMPLETED);
            order.setCompletedAt(Instant.now());
            order.setProviderTransactionNo(callback.providerTransactionNo());
            order.setProviderResponseCode(callback.responseCode());
            order.setProviderTransactionStatus(callback.transactionStatus());
            topupPortOut.save(order);
            return;
        }

        wallet.setAvailableBalance(wallet.getAvailableBalance().add(order.getAmount()));
        wallet.setUpdatedAt(Instant.now());
        walletPortOut.save(wallet);

        FinancialTransaction transaction = newTransaction(order);
        LedgerAccount clearing = requireAccount(LedgerAccountType.PLATFORM_CASH_CLEARING.name());
        LedgerAccount liability = requireAccount(LedgerAccountType.CUSTOMER_WALLET_LIABILITY.name());
        LedgerEntry debit = newEntry(transaction, clearing, LedgerEntrySide.DEBIT, order.getAmount(),
                "Wallet top-up " + order.getTransactionRef());
        LedgerEntry credit = newEntry(transaction, liability, LedgerEntrySide.CREDIT, order.getAmount(),
                "Wallet top-up " + order.getTransactionRef());
        postingPolicy.requireBalanced(List.of(debit, credit));
        ledgerPortOut.postBalancedTransaction(transaction, List.of(debit, credit));

        order.setStatus(WalletTopupStatus.COMPLETED);
        order.setCompletedAt(Instant.now());
        order.setProviderTransactionNo(callback.providerTransactionNo());
        order.setProviderResponseCode(callback.responseCode());
        order.setProviderTransactionStatus(callback.transactionStatus());
        topupPortOut.save(order);

        notifyTopupSucceededAfterCommit(order);
    }

    private Wallet provisionWallet(UUID customerId) {
        Wallet wallet = new Wallet();
        wallet.setWalletId(UUID.randomUUID());
        wallet.setOwnerType(com.ban.vehicle_management.shared.enumeration.billing.WalletOwnerType.CUSTOMER);
        wallet.setCustomerId(customerId);
        wallet.setWalletPurpose(WalletPurpose.PERSONAL);
        wallet.setCurrency(CURRENCY);
        wallet.setAvailableBalance(BigDecimal.ZERO);
        wallet.setPendingBalance(BigDecimal.ZERO);
        wallet.setHeldBalance(BigDecimal.ZERO);
        wallet.setStatus(WalletStatus.ACTIVE);
        wallet.setCreatedAt(Instant.now());
        return walletPortOut.createIfAbsent(wallet);
    }

    private void ensureOwnOrder(WalletTopupOrder order) {
        UUID customerId = walletAccessGuard.resolveCurrentApprovedCustomerId();
        Wallet wallet = walletPortOut.findById(order.getWalletId())
                .orElseThrow(() -> new NotFoundException("Wallet not found"));
        if (!customerId.equals(wallet.getCustomerId())) {
            throw new org.springframework.security.access.AccessDeniedException("Access is denied");
        }
    }

    private VnpayCallbackData verifyCallback(VnpayCallbackCommand command) {
        Map<String, String> parameters = command == null ? null : command.parameters();
        if (parameters == null || parameters.isEmpty()) {
            return new VnpayCallbackData(false, null, null, null, null, null, null, null, null, null);
        }
        return vnpayGatewayPortOut.verifyCallback(parameters);
    }

    private FinancialTransaction newTransaction(WalletTopupOrder order) {
        FinancialTransaction transaction = new FinancialTransaction();
        transaction.setFinancialTransactionId(UUID.randomUUID());
        transaction.setTransactionCode("TOPUP-" + order.getTransactionRef());
        transaction.setTransactionType(FinancialTransactionType.WALLET_TOP_UP);
        transaction.setStatus(FinancialTransactionStatus.POSTED);
        transaction.setIdempotencyKey("TOPUP-" + order.getTopupOrderId());
        transaction.setReferenceType("WALLET_TOPUP");
        transaction.setReferenceId(order.getTopupOrderId());
        transaction.setCurrency(CURRENCY);
        transaction.setOccurredAt(Instant.now());
        transaction.setCreatedAt(Instant.now());
        transaction.setCreatedBy(order.getCreatedBy());
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

    private void notifyTopupSucceededAfterCommit(WalletTopupOrder order) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            notifyTopupSucceeded(order);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                notifyTopupSucceeded(order);
            }
        });
    }

    private void notifyTopupSucceeded(WalletTopupOrder order) {
        try {
            if (notificationPortIn == null || order.getCreatedBy() == null) {
                return;
            }
            notificationPortIn.sendWebNotification(new SendNotificationCommand(
                    order.getCreatedBy(),
                    NotificationType.PAYMENT_SUCCEEDED, "Nạp ví thành công",
                    "Nạp " + order.getAmount().toPlainString() + " VND vào ví thành công.",
                    "billing", "wallet-topups", order.getTopupOrderId()));
        } catch (Exception ignored) {
            // Notifications must never break money movement.
        }
    }

    private com.ban.vehicle_management.shared.enumeration.billing.PaymentStatus mapToPaymentStatus(
            WalletTopupStatus status) {
        return switch (status) {
            case COMPLETED -> com.ban.vehicle_management.shared.enumeration.billing.PaymentStatus.SUCCESS;
            case FAILED, EXPIRED, CANCELLED ->
                com.ban.vehicle_management.shared.enumeration.billing.PaymentStatus.FAILED;
            case PENDING -> com.ban.vehicle_management.shared.enumeration.billing.PaymentStatus.PENDING;
        };
    }
}
