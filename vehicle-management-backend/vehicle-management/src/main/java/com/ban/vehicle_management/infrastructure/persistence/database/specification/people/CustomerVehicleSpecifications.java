package com.ban.vehicle_management.infrastructure.persistence.database.specification.people;

import com.ban.vehicle_management.infrastructure.persistence.database.entity.people.CustomerEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.people.CustomerVehicleEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.people.UserProfileEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.accesscontrol.SubscriptionEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.parking.ParkingSessionEntity;
import com.ban.vehicle_management.shared.enumeration.people.CustomerVehicleStatus;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Subquery;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;

public final class CustomerVehicleSpecifications {

    private CustomerVehicleSpecifications() {
    }

    /** Only vehicles actually used at an accessible lot are visible to its operator. */
    public static Specification<CustomerVehicleEntity> inParkingLots(Set<UUID> parkingLotIds) {
        return (root, query, cb) -> {
            if (parkingLotIds == null) {
                return cb.conjunction();
            }
            if (parkingLotIds.isEmpty()) {
                return cb.disjunction();
            }
            Subquery<UUID> subscriptions = query.subquery(UUID.class);
            var subscription = subscriptions.from(SubscriptionEntity.class);
            subscriptions.select(subscription.get("customerVehicleId"))
                    .where(cb.equal(subscription.get("customerVehicleId"), root.get("customerVehicleId")),
                            subscription.get("parkingLotId").in(parkingLotIds));
            Subquery<UUID> sessions = query.subquery(UUID.class);
            var session = sessions.from(ParkingSessionEntity.class);
            sessions.select(session.get("customerVehicleId"))
                    .where(cb.equal(session.get("customerVehicleId"), root.get("customerVehicleId")),
                            session.get("parkingLotId").in(parkingLotIds));
            return cb.or(cb.exists(subscriptions), cb.exists(sessions));
        };
    }

    public static Specification<CustomerVehicleEntity> withFilters(
            UUID customerId,
            CustomerVehicleStatus status,
            UUID vehicleTypeId,
            Boolean isDefault,
            String keyword
    ) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (customerId != null) {
                predicates.add(criteriaBuilder.equal(root.get("customerId"), customerId));
            }
            if (status != null) {
                predicates.add(criteriaBuilder.equal(root.get("status"), status));
            }
            if (vehicleTypeId != null) {
                predicates.add(criteriaBuilder.equal(root.get("vehicleTypeId"), vehicleTypeId));
            }
            if (isDefault != null) {
                predicates.add(criteriaBuilder.equal(root.get("isDefault"), isDefault));
            }
            if (hasText(keyword)) {
                String normalizedKeyword = "%" + keyword.trim().toLowerCase() + "%";
                String normalizedPlateKeyword = normalizePlateKeyword(keyword);
                Join<CustomerVehicleEntity, CustomerEntity> customerJoin = root.join("customer", JoinType.LEFT);
                Join<CustomerEntity, UserProfileEntity> userProfileJoin = customerJoin.join("userProfile", JoinType.LEFT);
                predicates.add(criteriaBuilder.or(
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("licensePlate")), normalizedKeyword),
                        normalizedPlateKeyword == null
                                ? criteriaBuilder.disjunction()
                                : criteriaBuilder.like(root.get("licensePlateNormalized"), "%" + normalizedPlateKeyword + "%"),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("brand")), normalizedKeyword),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("color")), normalizedKeyword),
                        criteriaBuilder.like(criteriaBuilder.lower(customerJoin.get("customerCode")), normalizedKeyword),
                        criteriaBuilder.like(criteriaBuilder.lower(userProfileJoin.get("fullName")), normalizedKeyword)
                ));
            }

            query.distinct(true);
            query.orderBy(criteriaBuilder.asc(root.get("licensePlate")));
            return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
        };
    }

    private static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private static String normalizePlateKeyword(String value) {
        String normalized = value.trim().toUpperCase().replace(" ", "").replace("-", "").replace(".", "");
        return normalized.length() >= 4 && normalized.matches("[A-Z0-9]+") ? normalized : null;
    }
}
