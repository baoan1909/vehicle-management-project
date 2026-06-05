package com.ban.vehicle_management.infrastructure.persistence.database.specification.iam;

import com.ban.vehicle_management.infrastructure.persistence.database.entity.iam.PermissionEntity;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public final class PermissionSpecifications {

    private PermissionSpecifications() {
    }

    public static Specification<PermissionEntity> withKeyword(String keyword) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (keyword != null && !keyword.isBlank()) {
                String keywordPattern = "%" + keyword.trim().toLowerCase() + "%";
                predicates.add(criteriaBuilder.or(
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("permissionCode")), keywordPattern),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("name")), keywordPattern),
                        criteriaBuilder.like(criteriaBuilder.lower(
                                criteriaBuilder.coalesce(root.get("description"), "")
                        ), keywordPattern)
                ));
            }

            query.orderBy(criteriaBuilder.asc(root.get("permissionCode")));
            return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
        };
    }
}
