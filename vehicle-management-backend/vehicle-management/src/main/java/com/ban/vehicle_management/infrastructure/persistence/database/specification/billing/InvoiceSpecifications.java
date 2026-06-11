package com.ban.vehicle_management.infrastructure.persistence.database.specification.billing;

import com.ban.vehicle_management.infrastructure.persistence.database.entity.billing.InvoiceEntity;
import com.ban.vehicle_management.shared.enumeration.billing.InvoiceStatus;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.UUID;

public final class InvoiceSpecifications {

    private InvoiceSpecifications(){}

    public static Specification<InvoiceEntity> withFilters(
            UUID customerId,
            UUID parkingSessionId,
            UUID subscriptionId,
            UUID lostCardReportId,
            InvoiceStatus status,
            Instant fromDate,
            Instant toDate,
            String keyword
    ){
        return Specification
                .where(hasCustomerId(customerId))
                .and(hasParkingSessionId(parkingSessionId))
                .and(hasSubscriptionId(subscriptionId))
                .and(hasLostCardReportId(lostCardReportId))
                .and(hasStatus(status))
                .and(issuedAtFrom(fromDate))
                .and(issuedTo(toDate))
                .and(containsKeyword(keyword));
    }

    private static Specification<InvoiceEntity> hasCustomerId(UUID customerId){
        return ((root, query, cb) -> customerId == null ? null : cb.equal(root.get("customerId"), customerId) );
    }

    private static Specification<InvoiceEntity> hasParkingSessionId(UUID parkingSessionId){
        return ((root, query, cb) -> parkingSessionId == null ? null : cb.equal(root.get("parkingSessionId"), parkingSessionId) );
    }

    private static Specification<InvoiceEntity> hasSubscriptionId(UUID subscriptionId){
        return ((root, query, cb) -> subscriptionId == null ? null : cb.equal(root.get("subscriptionId"), subscriptionId) );
    }

    private static Specification<InvoiceEntity> hasLostCardReportId(UUID lostCardReportId){
        return ((root, query, cb) -> lostCardReportId == null ? null : cb.equal(root.get("lostCardReportId"), lostCardReportId) );
    }

    private  static Specification<InvoiceEntity> hasStatus(InvoiceStatus invoiceStatus){
        return  ((root, query, cb) -> invoiceStatus == null ? null : cb.equal(root.get("status"), invoiceStatus));
    }

    private  static Specification<InvoiceEntity> issuedAtFrom(Instant fromDate){
        return  ((root, query, cb) -> fromDate == null ? null : cb.greaterThanOrEqualTo(root.get("issuedAt"), fromDate));
    }

    private  static Specification<InvoiceEntity> issuedTo(Instant toDate){
        return  ((root, query, cb) -> toDate == null ? null : cb.lessThanOrEqualTo(root.get("issuedAt"), toDate));
    }

    private static Specification<InvoiceEntity> containsKeyword(String keyword) {
        return (root, query, cb) -> {
            if (keyword == null || keyword.isBlank()) {
                return null;
            }

            String pattern = "%" + keyword.trim().toLowerCase() + "%";
            return cb.like(cb.lower(root.get("invoiceNo")), pattern);
        };
    }
}
