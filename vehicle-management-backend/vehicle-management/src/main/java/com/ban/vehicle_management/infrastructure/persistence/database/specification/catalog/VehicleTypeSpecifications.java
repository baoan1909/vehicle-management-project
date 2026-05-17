package com.ban.vehicle_management.infrastructure.persistence.database.specification.catalog;

import com.ban.vehicle_management.infrastructure.persistence.database.entity.catalog.VehicleTypeEntity;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;

public final class VehicleTypeSpecifications {

    private VehicleTypeSpecifications() {
    }

    public static Specification<VehicleTypeEntity> withFilters(Boolean isActive) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (isActive != null) {
                predicates.add(criteriaBuilder.equal(root.get("isActive"), isActive));
            }

            query.orderBy(criteriaBuilder.asc(root.get("code")));
            return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
        };
    }
}
