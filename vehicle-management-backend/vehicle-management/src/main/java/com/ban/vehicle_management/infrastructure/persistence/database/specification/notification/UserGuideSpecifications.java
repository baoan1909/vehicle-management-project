package com.ban.vehicle_management.infrastructure.persistence.database.specification.notification;

import com.ban.vehicle_management.infrastructure.persistence.database.entity.notification.UserGuideEntity;
import com.ban.vehicle_management.shared.enumeration.notification.UserGuideDisplayType;
import com.ban.vehicle_management.shared.enumeration.notification.UserGuideStatus;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;

public final class UserGuideSpecifications {

    private UserGuideSpecifications() {
    }

    public static Specification<UserGuideEntity> withFilters(
            UUID featureId,
            UserGuideStatus status,
            UserGuideDisplayType displayType,
            String keyword
    ) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (featureId != null) {
                predicates.add(criteriaBuilder.equal(root.get("featureId"), featureId));
            }

            if (status != null) {
                predicates.add(criteriaBuilder.equal(root.get("status"), status));
            }

            if (displayType != null) {
                predicates.add(criteriaBuilder.equal(root.get("displayType"), displayType));
            }

            if (keyword != null && !keyword.isBlank()) {
                String pattern = "%" + keyword.trim().toLowerCase(java.util.Locale.ROOT) + "%";
                predicates.add(criteriaBuilder.or(
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("code")), pattern),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("contextKey")), pattern)));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
