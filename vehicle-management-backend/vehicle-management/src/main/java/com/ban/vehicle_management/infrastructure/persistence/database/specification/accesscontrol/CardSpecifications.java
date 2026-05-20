package com.ban.vehicle_management.infrastructure.persistence.database.specification.accesscontrol;

import com.ban.vehicle_management.infrastructure.persistence.database.entity.accesscontrol.CardEntity;
import com.ban.vehicle_management.shared.enumeration.accesscontrol.CardStatus;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;

public final class CardSpecifications {

    private CardSpecifications() {
    }

    public static Specification<CardEntity> withFilters(
            CardStatus status,
            UUID cardTypeId,
            UUID vehicleTypeId,
            String keyword
    ) {
        return (root, query, criteriaBuilder) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();

            if (status != null) {
                predicates.add(criteriaBuilder.equal(root.get("status"), status));
            }
            if (cardTypeId != null) {
                predicates.add(criteriaBuilder.equal(root.get("cardTypeId"), cardTypeId));
            }
            if (vehicleTypeId != null) {
                predicates.add(criteriaBuilder.equal(root.get("vehicleTypeId"), vehicleTypeId));
            }
            if (keyword != null && !keyword.isBlank()) {
                String keywordPattern = "%" + keyword.trim().toLowerCase() + "%";
                predicates.add(criteriaBuilder.or(
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("cardNumber")), keywordPattern),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("uid")), keywordPattern)
                ));
            }

            query.orderBy(criteriaBuilder.asc(root.get("cardNumber")));
            return criteriaBuilder.and(predicates.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };
    }
}
