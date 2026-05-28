package com.ban.vehicle_management.infrastructure.persistence.database.specification.parking;

import com.ban.vehicle_management.infrastructure.persistence.database.entity.parking.ParkingLotEntity;
import com.ban.vehicle_management.shared.enumeration.parking.ParkingLotStatus;
import org.springframework.data.jpa.domain.Specification;

public final class ParkingLotSpecifications {

    private ParkingLotSpecifications() {
    }

    public static Specification<ParkingLotEntity> withFilters(ParkingLotStatus status, String keyword) {
        return Specification
                .where(hasStatus(status))
                .and(containsKeyword(keyword));
    }

    private static Specification<ParkingLotEntity> hasStatus(ParkingLotStatus status) {
        return (root, query, criteriaBuilder) ->
                status == null ? null : criteriaBuilder.equal(root.get("status"), status);
    }

    private static Specification<ParkingLotEntity> containsKeyword(String keyword) {
        return (root, query, criteriaBuilder) -> {
            if (keyword == null || keyword.isBlank()) {
                return null;
            }

            String pattern = "%" + keyword.trim().toLowerCase() + "%";
            return criteriaBuilder.or(
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("code")), pattern),
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("name")), pattern),
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("address")), pattern)
            );
        };
    }
}