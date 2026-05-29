package com.ban.vehicle_management.infrastructure.persistence.database.specification.parking;

import com.ban.vehicle_management.infrastructure.persistence.database.entity.parking.ZoneEntity;
import com.ban.vehicle_management.shared.enumeration.parking.ZoneStatus;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;

public final class ZoneSpecifications {

    private ZoneSpecifications() {
    }

    public static Specification<ZoneEntity> withFilters(
            UUID parkingLotId,
            UUID vehicleTypeId,
            ZoneStatus status,
            String keyword
    ) {
        return Specification
                .where(hasParkingLotId(parkingLotId))
                .and(hasVehicleTypeId(vehicleTypeId))
                .and(hasStatus(status))
                .and(containsKeyword(keyword));
    }

    private static Specification<ZoneEntity> hasParkingLotId(UUID parkingLotId) {
        return (root, query, criteriaBuilder) ->
                parkingLotId == null ? null : criteriaBuilder.equal(root.get("parkingLotId"), parkingLotId);
    }

    private static Specification<ZoneEntity> hasVehicleTypeId(UUID vehicleTypeId) {
        return (root, query, criteriaBuilder) ->
                vehicleTypeId == null ? null : criteriaBuilder.equal(root.get("vehicleTypeId"), vehicleTypeId);
    }

    private static Specification<ZoneEntity> hasStatus(ZoneStatus status) {
        return (root, query, criteriaBuilder) ->
                status == null ? null : criteriaBuilder.equal(root.get("status"), status);
    }

    private static Specification<ZoneEntity> containsKeyword(String keyword) {
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