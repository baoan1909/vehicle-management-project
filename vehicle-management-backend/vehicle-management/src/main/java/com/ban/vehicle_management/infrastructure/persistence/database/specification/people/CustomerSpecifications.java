package com.ban.vehicle_management.infrastructure.persistence.database.specification.people;

import com.ban.vehicle_management.infrastructure.persistence.database.entity.iam.AccountEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.people.CustomerEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.people.UserProfileEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.accesscontrol.SubscriptionEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.parking.ParkingSessionEntity;
import com.ban.vehicle_management.shared.enumeration.people.CustomerApprovalStatus;
import com.ban.vehicle_management.shared.enumeration.people.CustomerStatus;
import com.ban.vehicle_management.shared.enumeration.people.CustomerType;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Subquery;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;

public final class CustomerSpecifications {

    private CustomerSpecifications() {
    }

    /** Null is platform-wide; an empty set intentionally matches no customers. */
    public static Specification<CustomerEntity> inParkingLots(Set<UUID> parkingLotIds) {
        return (root, query, cb) -> {
            if (parkingLotIds == null) {
                return cb.conjunction();
            }
            if (parkingLotIds.isEmpty()) {
                return cb.disjunction();
            }
            Subquery<UUID> subscriptions = query.subquery(UUID.class);
            var subscription = subscriptions.from(SubscriptionEntity.class);
            subscriptions.select(subscription.get("customerId"))
                    .where(cb.equal(subscription.get("customerId"), root.get("customerId")),
                            subscription.get("parkingLotId").in(parkingLotIds));
            Subquery<UUID> sessions = query.subquery(UUID.class);
            var session = sessions.from(ParkingSessionEntity.class);
            sessions.select(session.get("customerId"))
                    .where(cb.equal(session.get("customerId"), root.get("customerId")),
                            session.get("parkingLotId").in(parkingLotIds));
            return cb.or(cb.exists(subscriptions), cb.exists(sessions));
        };
    }

    public static Specification<CustomerEntity> withFilters(
            CustomerStatus status,
            CustomerApprovalStatus approvalStatus,
            CustomerType customerType,
            String keyword
    ) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (query.getResultType() != Long.class && query.getResultType() != long.class) {
                root.fetch("userProfile", JoinType.LEFT).fetch("account", JoinType.LEFT);
            }

            if (status != null) {
                predicates.add(criteriaBuilder.equal(root.get("status"), status));
            }
            if (approvalStatus != null) {
                predicates.add(criteriaBuilder.equal(root.get("approvalStatus"), approvalStatus));
            }
            if (customerType != null) {
                predicates.add(criteriaBuilder.equal(root.get("customerType"), customerType));
            }
            if (keyword != null && !keyword.trim().isEmpty()) {
                String keywordPattern = "%" + keyword.trim().toLowerCase() + "%";
                Join<CustomerEntity, UserProfileEntity> userProfileJoin = root.join("userProfile", JoinType.LEFT);
                Join<UserProfileEntity, AccountEntity> accountJoin = userProfileJoin.join("account", JoinType.LEFT);
                predicates.add(criteriaBuilder.or(
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("customerCode")), keywordPattern),
                        criteriaBuilder.like(criteriaBuilder.lower(userProfileJoin.get("fullName")), keywordPattern),
                        criteriaBuilder.like(criteriaBuilder.lower(userProfileJoin.get("phoneNumber")), keywordPattern),
                        criteriaBuilder.like(criteriaBuilder.lower(accountJoin.get("email")), keywordPattern)
                ));
            }

            query.distinct(true);
            query.orderBy(criteriaBuilder.asc(root.get("customerCode")));
            return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
        };
    }
}
