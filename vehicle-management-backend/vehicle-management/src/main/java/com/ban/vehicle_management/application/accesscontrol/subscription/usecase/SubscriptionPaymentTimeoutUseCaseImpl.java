package com.ban.vehicle_management.application.accesscontrol.subscription.usecase;

import com.ban.vehicle_management.application.accesscontrol.subscription.config.SubscriptionTimeProperties;
import com.ban.vehicle_management.application.accesscontrol.card.port.out.CardPortOut;
import com.ban.vehicle_management.application.accesscontrol.subscription.port.in.SubscriptionPaymentTimeoutPortIn;
import com.ban.vehicle_management.application.accesscontrol.subscription.port.out.SubscriptionPortOut;
import com.ban.vehicle_management.application.billing.invoice.port.out.InvoicePortOut;
import com.ban.vehicle_management.application.billing.payment.port.out.PaymentPortOut;
import com.ban.vehicle_management.application.catalog.voucher.port.in.VoucherPortIn;
import com.ban.vehicle_management.domain.accesscontrol.card.model.Card;
import com.ban.vehicle_management.domain.accesscontrol.card.policy.CardPolicy;
import com.ban.vehicle_management.domain.accesscontrol.subscription.model.Subscription;
import com.ban.vehicle_management.domain.accesscontrol.subscription.policy.SubscriptionPolicy;
import com.ban.vehicle_management.domain.billing.invoice.model.Invoice;
import com.ban.vehicle_management.domain.billing.invoice.policy.InvoicePolicy;
import com.ban.vehicle_management.domain.billing.payment.model.Payment;
import com.ban.vehicle_management.domain.billing.payment.policy.PaymentPolicy;
import com.ban.vehicle_management.shared.enumeration.billing.InvoiceStatus;
import com.ban.vehicle_management.shared.enumeration.billing.PaymentStatus;
import com.ban.vehicle_management.shared.utils.DateTimeUtils;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SubscriptionPaymentTimeoutUseCaseImpl implements SubscriptionPaymentTimeoutPortIn {

    private static final String TIMEOUT_RESPONSE_CODE = "PAYMENT_TIMEOUT";
    private static final String TIMEOUT_TRANSACTION_STATUS = "EXPIRED";

    private final SubscriptionPortOut subscriptionPortOut;
    private final InvoicePortOut invoicePortOut;
    private final PaymentPortOut paymentPortOut;
    private final CardPortOut cardPortOut;
    private final VoucherPortIn voucherPortIn;
    private final Duration paymentTimeout;
    private final SubscriptionPolicy subscriptionPolicy = new SubscriptionPolicy();
    private final InvoicePolicy invoicePolicy = new InvoicePolicy();
    private final PaymentPolicy paymentPolicy = new PaymentPolicy();
    private final CardPolicy cardPolicy = new CardPolicy();

    @Autowired
    public SubscriptionPaymentTimeoutUseCaseImpl(
            SubscriptionPortOut subscriptionPortOut,
            InvoicePortOut invoicePortOut,
            PaymentPortOut paymentPortOut,
            CardPortOut cardPortOut,
            VoucherPortIn voucherPortIn,
            SubscriptionTimeProperties timeProperties
    ) {
        this(subscriptionPortOut, invoicePortOut, paymentPortOut, cardPortOut, voucherPortIn,
                timeProperties.getPaymentTimeout());
    }

    public SubscriptionPaymentTimeoutUseCaseImpl(
            SubscriptionPortOut subscriptionPortOut,
            InvoicePortOut invoicePortOut,
            PaymentPortOut paymentPortOut,
            CardPortOut cardPortOut,
            VoucherPortIn voucherPortIn,
            long paymentTimeoutHours
    ) {
        this(subscriptionPortOut, invoicePortOut, paymentPortOut, cardPortOut, voucherPortIn,
                Duration.ofHours(paymentTimeoutHours));
    }

    private SubscriptionPaymentTimeoutUseCaseImpl(
            SubscriptionPortOut subscriptionPortOut,
            InvoicePortOut invoicePortOut,
            PaymentPortOut paymentPortOut,
            CardPortOut cardPortOut,
            VoucherPortIn voucherPortIn,
            Duration paymentTimeout
    ) {
        if (paymentTimeout == null || paymentTimeout.isZero() || paymentTimeout.isNegative()) {
            throw new IllegalArgumentException("app.subscription.payment-timeout must be greater than zero");
        }
        this.subscriptionPortOut = subscriptionPortOut;
        this.invoicePortOut = invoicePortOut;
        this.paymentPortOut = paymentPortOut;
        this.cardPortOut = cardPortOut;
        this.voucherPortIn = voucherPortIn;
        this.paymentTimeout = paymentTimeout;
    }

    @Override
    @Transactional
    public int cancelExpiredPendingPayments(Instant now) {
        if (now == null) {
            throw new IllegalArgumentException("now must not be null");
        }

        Instant approvedAtCutoff = now.minus(paymentTimeout);
        LocalDate requestedEffectiveDateCutoff = DateTimeUtils.toAppLocalDate(now);
        List<Subscription> expiredSubscriptions = subscriptionPortOut.findExpiredPendingPaymentsForUpdate(
                approvedAtCutoff,
                requestedEffectiveDateCutoff
        );

        int cancelledCount = 0;
        for (Subscription subscription : expiredSubscriptions) {
            if (completeWhenInvoiceWasPaid(subscription, now)) {
                continue;
            }

            cancelInvoiceAndPendingPayments(subscription);
            voucherPortIn.releaseSubscriptionVoucher(
                    subscription.getSubscriptionId(),
                    "Subscription payment timed out",
                    now
            );
            releaseReservedCard(subscription);
            subscriptionPolicy.cancelBeforeRefundWorkflow(subscription);
            subscriptionPortOut.save(subscription);
            cancelledCount++;
        }
        return cancelledCount;
    }

    private boolean completeWhenInvoiceWasPaid(Subscription subscription, Instant now) {
        return invoicePortOut.findFirstBySubscriptionIdAndStatus(
                        subscription.getSubscriptionId(),
                        InvoiceStatus.PAID
                )
                .map(invoice -> {
                    voucherPortIn.redeemSubscriptionVoucher(subscription.getSubscriptionId(), now);
                    subscriptionPolicy.markPaymentCompleted(subscription);
                    subscriptionPortOut.save(subscription);
                    return true;
                })
                .orElse(false);
    }

    private void cancelInvoiceAndPendingPayments(Subscription subscription) {
        invoicePortOut.findFirstBySubscriptionIdAndStatus(
                        subscription.getSubscriptionId(),
                        InvoiceStatus.UNPAID
                )
                .ifPresent(invoice -> {
                    failPendingPayments(invoice);
                    invoicePolicy.cancel(invoice);
                    invoicePortOut.save(invoice);
                });
    }

    private void failPendingPayments(Invoice invoice) {
        paymentPortOut.findByInvoiceId(invoice.getInvoiceId())
                .stream()
                .filter(payment -> PaymentStatus.PENDING.equals(payment.getStatus()))
                .forEach(this::markPaymentExpired);
    }

    private void markPaymentExpired(Payment payment) {
        paymentPolicy.markVnpayFailed(
                payment,
                null,
                TIMEOUT_RESPONSE_CODE,
                TIMEOUT_TRANSACTION_STATUS,
                null,
                null
        );
        paymentPortOut.save(payment);
    }

    private void releaseReservedCard(Subscription subscription) {
        if (subscription.getCardId() == null) {
            return;
        }

        Card card = cardPortOut.findByIdForUpdate(subscription.getCardId()).orElse(null);
        if (card != null) {
            cardPolicy.release(card);
            cardPortOut.save(card);
        }
        subscription.setCardId(null);
    }
}
