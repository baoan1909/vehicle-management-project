package com.ban.vehicle_management.infrastructure.persistence.database.specification.catalog;

import com.ban.vehicle_management.infrastructure.persistence.database.entity.catalog.PricePlanEntity;
import com.ban.vehicle_management.shared.enumeration.catalog.PricePlanAppliesTo;
import jakarta.persistence.criteria.Predicate;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;

public final class PricePlanSpecifications {

    private PricePlanSpecifications() {
    }

    public static Specification<PricePlanEntity> withFilters(
            Boolean isActive,
            PricePlanAppliesTo appliesTo,
            LocalDate effectiveDate,
            String keyword
    ) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (isActive != null) {
                predicates.add(criteriaBuilder.equal(root.get("isActive"), isActive));
            }
            if (appliesTo != null) {
                predicates.add(criteriaBuilder.equal(root.get("appliesTo"), appliesTo));
            }
            if (effectiveDate != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("effectiveFrom"), effectiveDate));
                predicates.add(criteriaBuilder.or(
                        criteriaBuilder.isNull(root.get("effectiveTo")),
                        criteriaBuilder.greaterThanOrEqualTo(root.get("effectiveTo"), effectiveDate)
                ));
            }
            if (keyword != null && !keyword.isBlank()) {
                String keywordPattern = "%" + keyword.trim().toLowerCase() + "%";
                predicates.add(criteriaBuilder.or(
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("code")), keywordPattern),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("name")), keywordPattern)
                ));
            }

            query.orderBy(criteriaBuilder.asc(root.get("effectiveFrom")));
            return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
        };
    }
}