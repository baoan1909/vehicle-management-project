package com.ban.vehicle_management.infrastructure.persistence.database.specification.accesscontrol;

import com.ban.vehicle_management.infrastructure.persistence.database.entity.accesscontrol.CardEntity;
import com.ban.vehicle_management.shared.enumeration.accesscontrol.CardStatus;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.Set;
import org.springframework.data.jpa.domain.Specification;

public final class CardSpecifications {

    private CardSpecifications() {
    }

    public static Specification<CardEntity> withFilters(
            CardStatus status,
            UUID cardTypeId,
            String keyword,
            Set<UUID> parkingLotIds
    ) {
        return (root, query, criteriaBuilder) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();

            if (status != null) {
                predicates.add(criteriaBuilder.equal(root.get("status"), status));
            }
            if (cardTypeId != null) {
                predicates.add(criteriaBuilder.equal(root.get("cardTypeId"), cardTypeId));
            }
            if (parkingLotIds != null) {
                if (parkingLotIds.isEmpty()) {
                    predicates.add(criteriaBuilder.disjunction());
                } else {
                    predicates.add(root.get("parkingLotId").in(parkingLotIds));
                }
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

    public static Specification<CardEntity> withFilters(
            CardStatus status,
            UUID cardTypeId,
            String keyword
    ) {
        return withFilters(status, cardTypeId, keyword, null);
    }
}
