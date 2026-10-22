package com.ban.vehicle_management.infrastructure.persistence.database.specification.notification;

import com.ban.vehicle_management.infrastructure.persistence.database.entity.notification.UserGuideFeatureEntity;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;

public final class UserGuideFeatureSpecifications {

    private UserGuideFeatureSpecifications() {
    }

    public static Specification<UserGuideFeatureEntity> withFilters(Boolean enabled, String keyword) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (enabled != null) {
                predicates.add(criteriaBuilder.equal(root.get("enabled"), enabled));
            }

            if (keyword != null && !keyword.isBlank()) {
                String pattern = "%" + keyword.trim().toLowerCase(java.util.Locale.ROOT) + "%";
                predicates.add(criteriaBuilder.or(
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("code")), pattern),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("name")), pattern)));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
