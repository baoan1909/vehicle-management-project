package com.ban.vehicle_management.infrastructure.persistence.database.specification.people;

import com.ban.vehicle_management.infrastructure.persistence.database.entity.iam.AccountEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.people.EmployeeEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.people.UserProfileEntity;
import com.ban.vehicle_management.shared.enumeration.people.EmployeeStatus;
import com.ban.vehicle_management.shared.enumeration.people.UserProfileStatus;
import com.ban.vehicle_management.shared.enumeration.iam.AccountStatus;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;

public final class EmployeeSpecifications {

    private EmployeeSpecifications() {
    }

    public static Specification<EmployeeEntity> withFilters(EmployeeStatus status, String keyword) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            Join<EmployeeEntity, UserProfileEntity> visibleProfile = root.join("userProfile", JoinType.INNER);
            Join<UserProfileEntity, AccountEntity> visibleAccount = visibleProfile.join("account", JoinType.INNER);
            predicates.add(criteriaBuilder.notEqual(root.get("status"), EmployeeStatus.DELETED));
            predicates.add(criteriaBuilder.notEqual(visibleProfile.get("status"), UserProfileStatus.DELETED));
            predicates.add(criteriaBuilder.not(visibleAccount.get("status").in(
                    AccountStatus.PENDING_DELETION, AccountStatus.DELETED)));
            if (query.getResultType() != Long.class && query.getResultType() != long.class) {
                root.fetch("userProfile", JoinType.LEFT).fetch("account", JoinType.LEFT).fetch("role", JoinType.LEFT);
            }

            if (status != null) {
                predicates.add(criteriaBuilder.equal(root.get("status"), status));
            }

            if (hasText(keyword)) {
                String normalizedKeyword = "%" + keyword.trim().toLowerCase() + "%";
                predicates.add(criteriaBuilder.or(
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("employeeCode")), normalizedKeyword),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("jobTitle")), normalizedKeyword),
                        criteriaBuilder.like(criteriaBuilder.lower(visibleProfile.get("fullName")), normalizedKeyword),
                        criteriaBuilder.like(criteriaBuilder.lower(visibleAccount.get("email")), normalizedKeyword)
                ));
            }

            query.distinct(true);
            query.orderBy(criteriaBuilder.asc(root.get("employeeCode")));
            return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
        };
    }

    private static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
