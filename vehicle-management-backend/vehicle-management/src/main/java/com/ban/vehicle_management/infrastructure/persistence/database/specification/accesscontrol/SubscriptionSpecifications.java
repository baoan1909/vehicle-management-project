package com.ban.vehicle_management.infrastructure.persistence.database.specification.accesscontrol;

import com.ban.vehicle_management.infrastructure.persistence.database.entity.accesscontrol.SubscriptionEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.people.CustomerVehicleEntity;
import com.ban.vehicle_management.shared.enumeration.accesscontrol.SubscriptionStatus;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;

public final class SubscriptionSpecifications {

    private SubscriptionSpecifications() {
    }

    public static Specification<SubscriptionEntity> withFilters(
            UUID customerId,
            UUID customerVehicleId,
            UUID cardId,
            UUID ticketTypeId,
            SubscriptionStatus status,
            LocalDate effectiveFrom,
            LocalDate effectiveTo,
            String keyword
    ) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (customerId != null) {
                predicates.add(criteriaBuilder.equal(root.get("customerId"), customerId));
            }

            if (customerVehicleId != null) {
                predicates.add(criteriaBuilder.equal(root.get("customerVehicleId"), customerVehicleId));
            }

            if (cardId != null) {
                predicates.add(criteriaBuilder.equal(root.get("cardId"), cardId));
            }

            if (ticketTypeId != null) {
                predicates.add(criteriaBuilder.equal(root.get("ticketTypeId"), ticketTypeId));
            }

            if (status != null) {
                predicates.add(criteriaBuilder.equal(root.get("status"), status));
            }

            if (effectiveFrom != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("effectiveTo"), effectiveFrom));
            }

            if (effectiveTo != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("effectiveFrom"), effectiveTo));
            }

            if (keyword != null && !keyword.isBlank()) {
                Join<SubscriptionEntity, CustomerVehicleEntity> customerVehicle = root.join("customerVehicle", JoinType.LEFT);
                String keywordPattern = "%" + keyword.trim().toLowerCase() + "%";

                predicates.add(criteriaBuilder.or(
                        criteriaBuilder.like(criteriaBuilder.lower(customerVehicle.get("licensePlate")), keywordPattern)
                ));
            }

            query.orderBy(criteriaBuilder.desc(root.get("createdAt")));
            return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
        };
    }
}