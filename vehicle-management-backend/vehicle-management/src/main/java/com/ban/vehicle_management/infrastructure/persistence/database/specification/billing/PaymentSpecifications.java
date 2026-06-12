package com.ban.vehicle_management.infrastructure.persistence.database.specification.billing;

import com.ban.vehicle_management.infrastructure.persistence.database.entity.billing.PaymentEntity;
import com.ban.vehicle_management.shared.enumeration.billing.PaymentMethod;
import com.ban.vehicle_management.shared.enumeration.billing.PaymentStatus;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;

public final class PaymentSpecifications {

    private PaymentSpecifications() {
    }

    public static Specification<PaymentEntity> withFilters(
            UUID invoiceId,
            PaymentMethod paymentMethod,
            PaymentStatus status,
            UUID receivedBy,
            Instant fromDate,
            Instant toDate,
            String keyword
    ) {
        return Specification
                .where(hasInvoiceId(invoiceId))
                .and(hasPaymentMethod(paymentMethod))
                .and(hasStatus(status))
                .and(hasReceivedBy(receivedBy))
                .and(paidAtFrom(fromDate))
                .and(paidAtTo(toDate))
                .and(containsKeyword(keyword));
    }

    private static Specification<PaymentEntity> hasInvoiceId(UUID invoiceId) {
        return (root, query, cb) -> invoiceId == null ? null : cb.equal(root.get("invoiceId"), invoiceId);
    }

    private static Specification<PaymentEntity> hasPaymentMethod(PaymentMethod paymentMethod) {
        return (root, query, cb) -> paymentMethod == null ? null : cb.equal(root.get("paymentMethod"), paymentMethod);
    }

    private static Specification<PaymentEntity> hasStatus(PaymentStatus status) {
        return (root, query, cb) -> status == null ? null : cb.equal(root.get("status"), status);
    }

    private static Specification<PaymentEntity> hasReceivedBy(UUID receivedBy) {
        return (root, query, cb) -> receivedBy == null ? null : cb.equal(root.get("receivedBy"), receivedBy);
    }

    private static Specification<PaymentEntity> paidAtFrom(Instant fromDate) {
        return (root, query, cb) -> fromDate == null ? null : cb.greaterThanOrEqualTo(root.get("paidAt"), fromDate);
    }

    private static Specification<PaymentEntity> paidAtTo(Instant toDate) {
        return (root, query, cb) -> toDate == null ? null : cb.lessThanOrEqualTo(root.get("paidAt"), toDate);
    }

    private static Specification<PaymentEntity> containsKeyword(String keyword) {
        return (root, query, cb) -> {
            if (keyword == null || keyword.isBlank()) {
                return null;
            }

            String pattern = "%" + keyword.trim().toLowerCase() + "%";
            return cb.or(
                    cb.like(cb.lower(root.get("transactionRef")), pattern),
                    cb.like(cb.lower(root.get("note")), pattern)
            );
        };
    }
}