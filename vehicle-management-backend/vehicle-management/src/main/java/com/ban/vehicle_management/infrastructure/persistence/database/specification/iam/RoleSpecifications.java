package com.ban.vehicle_management.infrastructure.persistence.database.specification.iam;

import com.ban.vehicle_management.infrastructure.persistence.database.entity.iam.RoleEntity;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;

public final class RoleSpecifications {

    private RoleSpecifications() {
    }

    public static Specification<RoleEntity> withFilters(Boolean isActive, Boolean isSystem, String keyword) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (isActive != null) {
                predicates.add(criteriaBuilder.equal(root.get("isActive"), isActive));
            }

            if (isSystem != null) {
                predicates.add(criteriaBuilder.equal(root.get("isSystem"), isSystem));
            }

            if (keyword != null && !keyword.isBlank()) {
                String keywordPattern = "%" + keyword.trim().toLowerCase() + "%";
                predicates.add(criteriaBuilder.or(
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("code")), keywordPattern),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("name")), keywordPattern)
                ));
            }

            query.orderBy(criteriaBuilder.asc(root.get("code")));
            return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
        };
    }
}