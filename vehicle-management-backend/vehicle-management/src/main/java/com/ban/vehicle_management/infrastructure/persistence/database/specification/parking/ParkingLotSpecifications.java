package com.ban.vehicle_management.infrastructure.persistence.database.specification.parking;

import com.ban.vehicle_management.infrastructure.persistence.database.entity.parking.ParkingLotEntity;
import com.ban.vehicle_management.shared.enumeration.parking.ParkingLotStatus;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;

public final class ParkingLotSpecifications {

    private ParkingLotSpecifications() {
    }

    public static Specification<ParkingLotEntity> withFilters(
            ParkingLotStatus status,
            String keyword,
            Set<UUID> organizationIds,
            Set<UUID> parkingLotIds
    ) {
        return Specification
                .where(hasStatus(status))
                .and(containsKeyword(keyword))
                .and(hasOrganizationIds(organizationIds))
                .and(hasParkingLotIds(parkingLotIds));
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

    private static Specification<ParkingLotEntity> hasOrganizationIds(Set<UUID> organizationIds) {
        return (root, query, criteriaBuilder) -> {
            if (organizationIds == null) {
                return null;
            }
            return organizationIds.isEmpty()
                    ? criteriaBuilder.disjunction()
                    : root.get("organizationId").in(organizationIds);
        };
    }

    private static Specification<ParkingLotEntity> hasParkingLotIds(Set<UUID> parkingLotIds) {
        return (root, query, criteriaBuilder) -> {
            if (parkingLotIds == null) {
                return null;
            }
            return parkingLotIds.isEmpty()
                    ? criteriaBuilder.disjunction()
                    : root.get("parkingLotId").in(parkingLotIds);
        };
    }
}
