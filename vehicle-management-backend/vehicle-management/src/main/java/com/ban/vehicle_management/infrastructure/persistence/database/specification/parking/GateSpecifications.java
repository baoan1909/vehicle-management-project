package com.ban.vehicle_management.infrastructure.persistence.database.specification.parking;

import com.ban.vehicle_management.infrastructure.persistence.database.entity.parking.GateEntity;
import com.ban.vehicle_management.shared.enumeration.parking.GateStatus;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;

public final class GateSpecifications {

    private GateSpecifications() {
    }

    public static Specification<GateEntity> withFilters(UUID zoneId, GateStatus status, String keyword) {
        return Specification
                .where(hasZoneId(zoneId))
                .and(hasStatus(status))
                .and(containsKeyword(keyword));
    }

    private static Specification<GateEntity> hasZoneId(UUID zoneId) {
        return (root, query, criteriaBuilder) ->
                zoneId == null ? null : criteriaBuilder.equal(root.get("zoneId"), zoneId);
    }

    private static Specification<GateEntity> hasStatus(GateStatus status) {
        return (root, query, criteriaBuilder) ->
                status == null ? null : criteriaBuilder.equal(root.get("status"), status);
    }

    private static Specification<GateEntity> containsKeyword(String keyword) {
        return (root, query, criteriaBuilder) -> {
            if (keyword == null || keyword.isBlank()) {
                return null;
            }

            String pattern = "%" + keyword.trim().toLowerCase() + "%";

            return criteriaBuilder.or(
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("code")), pattern),
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("name")), pattern)
            );
        };
    }
}