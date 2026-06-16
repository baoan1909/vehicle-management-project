package com.ban.vehicle_management.application.billing.payment.usecase;

import com.ban.vehicle_management.application.accesscontrol.subscription.port.in.SubscriptionPortIn;
import com.ban.vehicle_management.application.billing.invoice.port.out.InvoicePortOut;
import com.ban.vehicle_management.application.billing.payment.authorization.PaymentAccessGuard;
import com.ban.vehicle_management.application.billing.payment.port.in.PaymentPortIn;
import com.ban.vehicle_management.application.billing.payment.port.out.PaymentPortOut;
import com.ban.vehicle_management.domain.billing.invoice.model.Invoice;
import com.ban.vehicle_management.domain.billing.invoice.policy.InvoicePolicy;
import com.ban.vehicle_management.domain.billing.payment.model.Payment;
import com.ban.vehicle_management.domain.billing.payment.policy.PaymentPolicy;
import com.ban.vehicle_management.shared.enumeration.billing.InvoiceStatus;
import com.ban.vehicle_management.shared.enumeration.billing.PaymentMethod;
import com.ban.vehicle_management.shared.enumeration.billing.PaymentStatus;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import com.ban.vehicle_management.shared.exception.ConflictException;
import com.ban.vehicle_management.shared.exception.NotFoundException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentUseCaseImpl implements PaymentPortIn {

    private final PaymentPortOut paymentPortOut;
    private final InvoicePortOut invoicePortOut;
    private final PaymentAccessGuard paymentAccessGuard;
    private final PaymentPolicy paymentPolicy = new PaymentPolicy();
    private final InvoicePolicy invoicePolicy = new InvoicePolicy();
    private final SubscriptionPortIn subscriptionPortIn;

    public PaymentUseCaseImpl(
            PaymentPortOut paymentPortOut,
            InvoicePortOut invoicePortOut,
            PaymentAccessGuard paymentAccessGuard,
            SubscriptionPortIn subscriptionPortIn
    ) {
        this.paymentPortOut = paymentPortOut;
        this.invoicePortOut = invoicePortOut;
        this.paymentAccessGuard = paymentAccessGuard;
        this.subscriptionPortIn = subscriptionPortIn;
    }

    @Override
    @Transactional
    public Payment recordPayment(UUID invoiceId, Payment payment) {
        UUID receivedBy = paymentAccessGuard.requireCanCreateAndGetAccountId();

        Invoice invoice = invoicePortOut.findById(invoiceId)
                .orElseThrow(() -> new NotFoundException("Invoice not found"));

        validatePayableInvoice(invoice);
        validatePaymentAmount(payment, invoice);
        validateNoSuccessfulPayment(invoiceId);

        payment.setPaymentId(UUID.randomUUID());
        paymentPolicy.initializeSuccessfulPayment(payment, invoiceId, receivedBy, Instant.now());

        validateTransactionRefUnique(payment);

        Payment savedPayment = paymentPortOut.save(payment);

        invoicePolicy.markPaid(invoice, savedPayment.getPaidAt());
        invoicePortOut.save(invoice);

        if (invoice.getSubscriptionId() != null) {
            subscriptionPortIn.markSubscriptionPaymentCompleted(invoice.getSubscriptionId());
        }

        return savedPayment;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Payment> getPayments(
            UUID invoiceId,
            PaymentMethod paymentMethod,
            PaymentStatus status,
            UUID receivedBy,
            Instant fromDate,
            Instant toDate,
            String keyword
    ) {
        paymentAccessGuard.ensureCanReadAll();
        return paymentPortOut.findAll(
                invoiceId,
                paymentMethod,
                status,
                receivedBy,
                fromDate,
                toDate,
                normalizeKeyword(keyword)
        );
    }

    private void validatePayableInvoice(Invoice invoice) {
        if (InvoiceStatus.PAID.equals(invoice.getStatus())) {
            throw new ConflictException("Only unpaid invoice can be paid");
        }

        if (InvoiceStatus.CANCELLED.equals(invoice.getStatus())) {
            throw new ConflictException("Cancelled invoice cannot be paid");
        }

        if (InvoiceStatus.REFUNDED.equals(invoice.getStatus())) {
            throw new ConflictException("Refunded invoice cannot be paid");
        }

        if (!InvoiceStatus.UNPAID.equals(invoice.getStatus())) {
            throw new ConflictException("Only unpaid invoice can be paid");
        }

        if (invoice.getFinalAmount().compareTo(BigDecimal.ZERO) == 0) {
            throw new BadRequestException("Zero amount invoice does not require payment");
        }
    }

    private void validatePaymentAmount(Payment payment, Invoice invoice) {
        if (payment.getAmount() == null) {
            throw new BadRequestException("amount must not be null");
        }

        if (payment.getAmount().compareTo(invoice.getFinalAmount()) != 0) {
            throw new BadRequestException("Payment amount must equal invoice final amount");
        }
    }

    private void validateNoSuccessfulPayment(UUID invoiceId) {
        if (paymentPortOut.existsByInvoiceIdAndStatus(invoiceId, PaymentStatus.SUCCESS)) {
            throw new ConflictException("Successful payment already exists for this invoice");
        }
    }

    private void validateTransactionRefUnique(Payment payment) {
        if (PaymentMethod.CASH.equals(payment.getPaymentMethod()) || payment.getTransactionRef() == null) {
            return;
        }

        if (paymentPortOut.existsByTransactionRefAndStatus(payment.getTransactionRef(), PaymentStatus.SUCCESS)) {
            throw new ConflictException("transactionRef already exists");
        }
    }

    private String normalizeKeyword(String keyword) {
        return keyword == null || keyword.isBlank() ? null : keyword.trim();
    }
}