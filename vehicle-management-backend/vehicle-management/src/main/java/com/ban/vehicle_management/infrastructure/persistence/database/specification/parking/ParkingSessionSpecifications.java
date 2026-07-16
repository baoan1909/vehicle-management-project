package com.ban.vehicle_management.infrastructure.persistence.database.specification.parking;

import com.ban.vehicle_management.infrastructure.persistence.database.entity.accesscontrol.CardEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.catalog.VehicleTypeEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.parking.ParkingSessionEntity;
import com.ban.vehicle_management.shared.enumeration.parking.ParkingSessionStatus;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.hibernate.query.criteria.JpaExpression;
import org.springframework.data.jpa.domain.Specification;

public final class ParkingSessionSpecifications {

    private ParkingSessionSpecifications() {
    }

    public static Specification<ParkingSessionEntity> withFilters(
            ParkingSessionStatus status,
            UUID vehicleTypeId,
            UUID zoneId,
            Instant checkInFrom,
            Instant checkInTo,
            String keyword,
            List<UUID> customerVehicleIds
    ) {
        return Specification
                .where(distinct())
                .and(hasStatus(status))
                .and(hasVehicleType(vehicleTypeId))
                .and(hasCustomerVehicleIn(customerVehicleIds))
                .and(hasZone(zoneId))
                .and(checkInFrom(checkInFrom))
                .and(checkInTo(checkInTo))
                .and(containsKeyword(keyword));
    }

    private static Specification<ParkingSessionEntity> distinct() {
        return (root, query, cb) -> {
            if (query != null && query.getResultType() != Long.class && query.getResultType() != long.class) {
                query.distinct(true);
            }
            return null;
        };
    }

    private static Specification<ParkingSessionEntity> hasStatus(ParkingSessionStatus status) {
        return (root, query, cb) -> status == null ? null : cb.equal(root.get("status"), status);
    }

    private static Specification<ParkingSessionEntity> hasVehicleType(UUID vehicleTypeId) {
        return (root, query, cb) -> vehicleTypeId == null ? null : cb.equal(root.get("vehicleTypeId"), vehicleTypeId);
    }

    private static Specification<ParkingSessionEntity> hasZone(UUID zoneId) {
        return (root, query, cb) -> zoneId == null ? null : cb.equal(root.get("zoneId"), zoneId);
    }

    private static Specification<ParkingSessionEntity> hasCustomerVehicleIn(List<UUID> customerVehicleIds) {
        return (root, query, cb) -> {
            if (customerVehicleIds == null) {
                return null;
            }
            if (customerVehicleIds.isEmpty()) {
                return cb.disjunction();
            }
            return root.get("customerVehicleId").in(customerVehicleIds);
        };
    }

    private static Specification<ParkingSessionEntity> checkInFrom(Instant checkInFrom) {
        return (root, query, cb) -> checkInFrom == null ? null : cb.greaterThanOrEqualTo(root.get("checkInTime"), checkInFrom);
    }

    private static Specification<ParkingSessionEntity> checkInTo(Instant checkInTo) {
        return (root, query, cb) -> checkInTo == null ? null : cb.lessThan(root.get("checkInTime"), checkInTo);
    }

    private static Specification<ParkingSessionEntity> containsKeyword(String keyword) {
        return (root, query, cb) -> {
            if (keyword == null || keyword.isBlank()) {
                return null;
            }

            String pattern = "%" + keyword.trim().toLowerCase() + "%";
            Join<ParkingSessionEntity, CardEntity> card = root.join("card", JoinType.LEFT);
            Join<ParkingSessionEntity, VehicleTypeEntity> vehicleType = root.join("vehicleType", JoinType.LEFT);

            return cb.or(
                    cb.like(cb.lower(castToString(root.get("parkingSessionId"))), pattern),
                    cb.like(cb.lower(root.get("licensePlateIn")), pattern),
                    cb.like(cb.lower(cb.coalesce(root.get("licensePlateOut"), "")), pattern),
                    cb.like(cb.lower(card.get("cardNumber")), pattern),
                    cb.like(cb.lower(card.get("uid")), pattern),
                    cb.like(cb.lower(cb.coalesce(vehicleType.get("code"), "")), pattern),
                    cb.like(cb.lower(cb.coalesce(vehicleType.get("name"), "")), pattern)
            );
        };
    }

    private static Expression<String> castToString(Expression<?> expression) {
        return ((JpaExpression<?>) expression).cast(String.class);
    }
}
