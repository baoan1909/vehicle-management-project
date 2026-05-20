package com.ban.vehicle_management.infrastructure.persistence.database.specification.people;

import com.ban.vehicle_management.infrastructure.persistence.database.entity.people.EmployeeEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.people.UserProfileEntity;
import com.ban.vehicle_management.shared.enumeration.people.EmployeeStatus;
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

            if (status != null) {
                predicates.add(criteriaBuilder.equal(root.get("status"), status));
            }

            if (hasText(keyword)) {
                String normalizedKeyword = "%" + keyword.trim().toLowerCase() + "%";
                Join<EmployeeEntity, UserProfileEntity> userProfileJoin = root.join("userProfile", JoinType.LEFT);
                predicates.add(criteriaBuilder.or(
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("employeeCode")), normalizedKeyword),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("jobTitle")), normalizedKeyword),
                        criteriaBuilder.like(criteriaBuilder.lower(userProfileJoin.get("fullName")), normalizedKeyword)
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
