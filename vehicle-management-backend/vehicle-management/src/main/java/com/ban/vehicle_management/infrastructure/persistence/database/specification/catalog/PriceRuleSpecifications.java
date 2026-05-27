package com.ban.vehicle_management.infrastructure.persistence.database.specification.catalog;

import com.ban.vehicle_management.infrastructure.persistence.database.entity.catalog.PriceRuleEntity;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;

public final class PriceRuleSpecifications {

    private PriceRuleSpecifications() {
    }

    public static Specification<PriceRuleEntity> withFilters(
            UUID pricePlanId,
            UUID vehicleTypeId,
            UUID ticketTypeId,
            Boolean isActive,
            String keyword
    ) {
        return Specification
                .where(hasPricePlanId(pricePlanId))
                .and(hasVehicleTypeId(vehicleTypeId))
                .and(hasTicketTypeId(ticketTypeId))
                .and(hasActiveStatus(isActive))
                .and(containsKeyword(keyword));
    }

    private static Specification<PriceRuleEntity> hasPricePlanId(UUID pricePlanId) {
        return (root, query, criteriaBuilder) ->
                pricePlanId == null ? null : criteriaBuilder.equal(root.get("pricePlanId"), pricePlanId);
    }

    private static Specification<PriceRuleEntity> hasVehicleTypeId(UUID vehicleTypeId) {
        return (root, query, criteriaBuilder) ->
                vehicleTypeId == null ? null : criteriaBuilder.equal(root.get("vehicleTypeId"), vehicleTypeId);
    }

    private static Specification<PriceRuleEntity> hasTicketTypeId(UUID ticketTypeId) {
        return (root, query, criteriaBuilder) ->
                ticketTypeId == null ? null : criteriaBuilder.equal(root.get("ticketTypeId"), ticketTypeId);
    }

    private static Specification<PriceRuleEntity> hasActiveStatus(Boolean isActive) {
        return (root, query, criteriaBuilder) ->
                isActive == null ? null : criteriaBuilder.equal(root.get("isActive"), isActive);
    }

    private static Specification<PriceRuleEntity> containsKeyword(String keyword) {
        return (root, query, criteriaBuilder) -> {
            if (keyword == null || keyword.isBlank()) {
                return null;
            }

            String pattern = "%" + keyword.trim().toLowerCase() + "%";
            return criteriaBuilder.like(criteriaBuilder.lower(root.get("ruleName")), pattern);
        };
    }
}