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

    public void initializePendingVnpayPayment(
            Payment payment,
            UUID invoiceId,
            BigDecimal amount,
            String transactionRef,
            Instant createdAt,
            Instant expiresAt
    ) {
        require(payment, "payment");
        require(invoiceId, "invoiceId");
        require(amount, "amount");
        require(createdAt, "createdAt");
        require(expiresAt, "expiresAt");

        if (!expiresAt.isAfter(createdAt)) {
            throw new BadRequestException("expiresAt must be after createdAt");
        }

        payment.setInvoiceId(invoiceId);
        payment.setPaymentMethod(PaymentMethod.VNPAY);
        payment.setAmount(amount);
        payment.setTransactionRef(normalizeTransactionRef(PaymentMethod.VNPAY, transactionRef));
        payment.setStatus(PaymentStatus.PENDING);
        payment.setPaidAt(null);
        payment.setReceivedBy(null);
        payment.setNote("VNPAY online payment");
        payment.setCreatedAt(createdAt);
        payment.setExpiresAt(expiresAt);

        validateState(payment);
    }

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

    public void markVnpaySuccessful(
            Payment payment,
            Instant paidAt,
            String providerTransactionNo,
            String responseCode,
            String transactionStatus,
            String bankCode,
            String cardType
    ) {
        requirePendingVnpayPayment(payment);
        require(paidAt, "paidAt");

        payment.setStatus(PaymentStatus.SUCCESS);
        payment.setPaidAt(paidAt);
        applyProviderResult(
                payment,
                providerTransactionNo,
                responseCode,
                transactionStatus,
                bankCode,
                cardType
        );
        payment.setFailureReason(null);

        validateState(payment);
    }

    public void markVnpayFailed(
            Payment payment,
            String providerTransactionNo,
            String responseCode,
            String transactionStatus,
            String bankCode,
            String cardType
    ) {
        requirePendingVnpayPayment(payment);

        payment.setStatus(PaymentStatus.FAILED);
        payment.setPaidAt(null);
        applyProviderResult(
                payment,
                providerTransactionNo,
                responseCode,
                transactionStatus,
                bankCode,
                cardType
        );
        payment.setFailureReason(TextValidationUtils.normalizeNullableText(
                "VNPAY response code " + responseCode,
                "failureReason",
                255
        ));

        validateState(payment);
    }

    public void validateState(Payment payment) {
        require(payment, "payment");
        require(payment.getPaymentId(), "paymentId");
        require(payment.getInvoiceId(), "invoiceId");
        require(payment.getPaymentMethod(), "paymentMethod");
        require(payment.getAmount(), "amount");
        require(payment.getStatus(), "status");

        if (payment.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Payment amount must be greater than zero");
        }

        if (!PaymentMethod.CASH.equals(payment.getPaymentMethod()) && payment.getTransactionRef() == null) {
            throw new BadRequestException("transactionRef is required for non-cash payment");
        }

        if (PaymentStatus.PENDING.equals(payment.getStatus())) {
            if (!PaymentMethod.VNPAY.equals(payment.getPaymentMethod())) {
                throw new BadRequestException("Only VNPAY payment can be pending");
            }
            if (payment.getPaidAt() != null || payment.getReceivedBy() != null) {
                throw new BadRequestException("Pending payment cannot have paidAt or receivedBy");
            }
            require(payment.getCreatedAt(), "createdAt");
            require(payment.getExpiresAt(), "expiresAt");
            return;
        }

        if (PaymentStatus.SUCCESS.equals(payment.getStatus())) {
            require(payment.getPaidAt(), "paidAt");
            if (PaymentMethod.CASH.equals(payment.getPaymentMethod())) {
                require(payment.getReceivedBy(), "receivedBy");
            }
            return;
        }

        if (PaymentStatus.FAILED.equals(payment.getStatus())) {
            if (payment.getPaidAt() != null) {
                throw new BadRequestException("Failed payment cannot have paidAt");
            }
            return;
        }

        if (!PaymentStatus.REFUNDED.equals(payment.getStatus())) {
            throw new BadRequestException("Unsupported payment status");
        }
    }

    private String normalizeTransactionRef(PaymentMethod paymentMethod, String transactionRef) {
        String normalized = TextValidationUtils.normalizeNullableText(transactionRef, "transactionRef", 100);

        if (!PaymentMethod.CASH.equals(paymentMethod) && normalized == null) {
            throw new BadRequestException("transactionRef is required for non-cash payment");
        }

        return normalized;
    }

    private void requirePendingVnpayPayment(Payment payment) {
        require(payment, "payment");
        if (!PaymentMethod.VNPAY.equals(payment.getPaymentMethod())
                || !PaymentStatus.PENDING.equals(payment.getStatus())) {
            throw new BadRequestException("Only pending VNPAY payment can be updated");
        }
    }

    private void applyProviderResult(
            Payment payment,
            String providerTransactionNo,
            String responseCode,
            String transactionStatus,
            String bankCode,
            String cardType
    ) {
        payment.setProviderTransactionNo(TextValidationUtils.normalizeNullableText(
                providerTransactionNo,
                "providerTransactionNo",
                100
        ));
        payment.setProviderResponseCode(TextValidationUtils.normalizeNullableText(
                responseCode,
                "providerResponseCode",
                20
        ));
        payment.setProviderTransactionStatus(TextValidationUtils.normalizeNullableText(
                transactionStatus,
                "providerTransactionStatus",
                20
        ));
        payment.setBankCode(TextValidationUtils.normalizeNullableText(bankCode, "bankCode", 20));
        payment.setCardType(TextValidationUtils.normalizeNullableText(cardType, "cardType", 30));
    }

    private void require(Object value, String fieldName) {
        if (value == null) {
            throw new BadRequestException(fieldName + " must not be null");
        }
    }
}
