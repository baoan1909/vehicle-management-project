package com.ban.vehicle_management.infrastructure.persistence.database.specification.people;

import com.ban.vehicle_management.infrastructure.persistence.database.entity.people.UserProfileEntity;
import com.ban.vehicle_management.shared.enumeration.people.UserProfileStatus;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public final class UserProfileSpecifications {

    private UserProfileSpecifications() {
    }

    public static Specification<UserProfileEntity> withFilters(UserProfileStatus status, String keyword) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(criteriaBuilder.notEqual(root.get("status"), UserProfileStatus.DELETED));

            var account = root.join("account", jakarta.persistence.criteria.JoinType.LEFT);
            predicates.add(criteriaBuilder.or(
                    criteriaBuilder.isNull(account.get("accountId")),
                    criteriaBuilder.not(account.get("status").in(
                            com.ban.vehicle_management.shared.enumeration.iam.AccountStatus.PENDING_DELETION,
                            com.ban.vehicle_management.shared.enumeration.iam.AccountStatus.DELETED))
            ));

            if (status != null) {
                predicates.add(criteriaBuilder.equal(root.get("status"), status));
            }

            if (hasText(keyword)) {
                String normalizedKeyword = "%" + keyword.trim().toLowerCase() + "%";
                predicates.add(criteriaBuilder.or(
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("fullName")), normalizedKeyword),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("phoneNumber")), normalizedKeyword),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("identifyCard")), normalizedKeyword)
                ));
            }

            query.orderBy(criteriaBuilder.asc(root.get("fullName")));
            return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
        };
    }

    public static Specification<UserProfileEntity> businessVisible() {
        return withFilters(null, null);
    }

    private static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
