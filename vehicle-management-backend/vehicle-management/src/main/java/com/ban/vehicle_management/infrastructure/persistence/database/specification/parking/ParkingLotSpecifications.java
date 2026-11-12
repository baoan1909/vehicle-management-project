package com.ban.vehicle_management.infrastructure.persistence.database.specification.parking;

import com.ban.vehicle_management.infrastructure.persistence.database.entity.parking.ParkingLotEntity;
import com.ban.vehicle_management.shared.enumeration.parking.ParkingLotStatus;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.iam.OrganizationEntity;
import com.ban.vehicle_management.shared.enumeration.iam.OrganizationStatus;

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
                .where(isBusinessVisible())
                .and(hasStatus(status))
                .and(containsKeyword(keyword))
                .and(hasOrganizationIds(organizationIds))
                .and(hasParkingLotIds(parkingLotIds));
    }

    private static Specification<ParkingLotEntity> isBusinessVisible() {
        return (root, query, criteriaBuilder) -> {
            var organization = query.subquery(UUID.class);
            var organizationRoot = organization.from(OrganizationEntity.class);
            organization.select(organizationRoot.get("organizationId"));
            organization.where(
                    criteriaBuilder.equal(
                            organizationRoot.get("organizationId"), root.get("organizationId")),
                    organizationRoot.get("status").in(
                            OrganizationStatus.ACTIVE, OrganizationStatus.SUSPENDED));
            return criteriaBuilder.and(
                    criteriaBuilder.notEqual(root.get("status"), ParkingLotStatus.CLOSED),
                    criteriaBuilder.exists(organization));
        };
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
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("addressDisplay")), pattern)
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
