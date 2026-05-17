package com.ban.vehicle_management.infrastructure.persistence.database.specification.people;

import com.ban.vehicle_management.infrastructure.persistence.database.entity.people.UserProfileEntity;
import com.ban.vehicle_management.shared.enumeration.UserProfileStatus;
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

    private static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
