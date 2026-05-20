package com.ban.vehicle_management.infrastructure.persistence.database.specification.people;

import com.ban.vehicle_management.infrastructure.persistence.database.entity.people.CustomerEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.people.UserProfileEntity;
import com.ban.vehicle_management.shared.enumeration.people.CustomerApprovalStatus;
import com.ban.vehicle_management.shared.enumeration.people.CustomerStatus;
import com.ban.vehicle_management.shared.enumeration.people.CustomerType;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;

public final class CustomerSpecifications {

    private CustomerSpecifications() {
    }

    public static Specification<CustomerEntity> withFilters(
            CustomerStatus status,
            CustomerApprovalStatus approvalStatus,
            CustomerType customerType,
            String keyword
    ) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (status != null) {
                predicates.add(criteriaBuilder.equal(root.get("status"), status));
            }
            if (approvalStatus != null) {
                predicates.add(criteriaBuilder.equal(root.get("approvalStatus"), approvalStatus));
            }
            if (customerType != null) {
                predicates.add(criteriaBuilder.equal(root.get("customerType"), customerType));
            }
            if (keyword != null && !keyword.trim().isEmpty()) {
                String keywordPattern = "%" + keyword.trim().toLowerCase() + "%";
                Join<CustomerEntity, UserProfileEntity> userProfileJoin = root.join("userProfile", JoinType.LEFT);
                predicates.add(criteriaBuilder.or(
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("customerCode")), keywordPattern),
                        criteriaBuilder.like(criteriaBuilder.lower(userProfileJoin.get("fullName")), keywordPattern),
                        criteriaBuilder.like(criteriaBuilder.lower(userProfileJoin.get("phoneNumber")), keywordPattern)
                ));
            }

            query.distinct(true);
            query.orderBy(criteriaBuilder.asc(root.get("customerCode")));
            return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
        };
    }
}
