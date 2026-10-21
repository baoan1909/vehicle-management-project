package com.ban.vehicle_management.infrastructure.persistence.database.specification.billing;

import com.ban.vehicle_management.infrastructure.persistence.database.entity.billing.PlatformFeeTypeEntity;
import com.ban.vehicle_management.shared.enumeration.billing.PlatformFeeBasis;
import com.ban.vehicle_management.shared.enumeration.billing.PlatformFeeTransactionType;
import com.ban.vehicle_management.shared.enumeration.billing.PlatformFeeTypeStatus;
import org.springframework.data.jpa.domain.Specification;

public final class PlatformFeeTypeSpecifications {

    private PlatformFeeTypeSpecifications() {
    }

    public static Specification<PlatformFeeTypeEntity> withFilters(
            String keyword, PlatformFeeTypeStatus status,
            PlatformFeeTransactionType transactionType, PlatformFeeBasis feeBasis) {
        return Specification.where(hasStatus(status))
                .and(hasTransactionType(transactionType))
                .and(hasFeeBasis(feeBasis))
                .and(containsKeyword(keyword));
    }

    private static Specification<PlatformFeeTypeEntity> hasStatus(PlatformFeeTypeStatus status) {
        return (root, query, cb) -> status == null ? null : cb.equal(root.get("status"), status);
    }

    private static Specification<PlatformFeeTypeEntity> hasTransactionType(PlatformFeeTransactionType transactionType) {
        return (root, query, cb) -> transactionType == null ? null : cb.equal(root.get("transactionType"), transactionType);
    }

    private static Specification<PlatformFeeTypeEntity> hasFeeBasis(PlatformFeeBasis feeBasis) {
        return (root, query, cb) -> feeBasis == null ? null : cb.equal(root.get("feeBasis"), feeBasis);
    }

    private static Specification<PlatformFeeTypeEntity> containsKeyword(String keyword) {
        return (root, query, cb) -> {
            if (keyword == null || keyword.isBlank()) {
                return null;
            }
            String pattern = "%" + keyword.trim().toLowerCase() + "%";
            return cb.or(
                    cb.like(cb.lower(root.get("feeCode")), pattern),
                    cb.like(cb.lower(root.get("name")), pattern));
        };
    }
}
