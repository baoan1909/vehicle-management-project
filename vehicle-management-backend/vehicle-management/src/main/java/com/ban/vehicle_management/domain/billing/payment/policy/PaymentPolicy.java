package com.ban.vehicle_management.domain.billing.payment.policy;

import com.ban.vehicle_management.domain.billing.payment.model.Payment;
import com.ban.vehicle_management.shared.enumeration.billing.PaymentMethod;
import com.ban.vehicle_management.shared.enumeration.billing.PaymentStatus;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import com.ban.vehicle_management.shared.utils.TextValidationUtils;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class PaymentPolicy {

    public void initializeSuccessfulPayment(Payment payment, UUID invoiceId, UUID receivedBy, Instant paidAt) {
        require(payment, "payment");
        require(invoiceId, "invoiceId");
        require(receivedBy, "receivedBy");
        require(paidAt, "paidAt");

        payment.setInvoiceId(invoiceId);
        payment.setReceivedBy(receivedBy);
        payment.setPaidAt(paidAt);
        payment.setStatus(PaymentStatus.SUCCESS);
        payment.setTransactionRef(normalizeTransactionRef(payment.getPaymentMethod(), payment.getTransactionRef()));
        payment.setNote(TextValidationUtils.normalizeNullableText(payment.getNote(), "note", 255));

        validateState(payment);
    }

    public void validateState(Payment payment) {
        require(payment, "payment");
        require(payment.getPaymentId(), "paymentId");
        require(payment.getInvoiceId(), "invoiceId");
        require(payment.getPaymentMethod(), "paymentMethod");
        require(payment.getAmount(), "amount");
        require(payment.getStatus(), "status");
        require(payment.getPaidAt(), "paidAt");
        require(payment.getReceivedBy(), "receivedBy");

        if (payment.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Payment amount must be greater than zero");
        }

        if (!PaymentStatus.SUCCESS.equals(payment.getStatus())) {
            throw new BadRequestException("MVP payment only supports SUCCESS status");
        }

        if (!PaymentMethod.CASH.equals(payment.getPaymentMethod()) && payment.getTransactionRef() == null) {
            throw new BadRequestException("transactionRef is required for non-cash payment");
        }
    }

    private String normalizeTransactionRef(PaymentMethod paymentMethod, String transactionRef) {
        String normalized = TextValidationUtils.normalizeNullableText(transactionRef, "transactionRef", 100);

        if (!PaymentMethod.CASH.equals(paymentMethod) && normalized == null) {
            throw new BadRequestException("transactionRef is required for non-cash payment");
        }

        return normalized;
    }

    private void require(Object value, String fieldName) {
        if (value == null) {
            throw new BadRequestException(fieldName + " must not be null");
        }
    }
}