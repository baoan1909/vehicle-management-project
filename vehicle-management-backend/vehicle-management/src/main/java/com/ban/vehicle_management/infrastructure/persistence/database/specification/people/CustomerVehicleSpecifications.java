package com.ban.vehicle_management.infrastructure.persistence.database.specification.people;

import com.ban.vehicle_management.infrastructure.persistence.database.entity.people.CustomerEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.people.CustomerVehicleEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.people.UserProfileEntity;
import com.ban.vehicle_management.shared.enumeration.people.CustomerVehicleStatus;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;

public final class CustomerVehicleSpecifications {

    private CustomerVehicleSpecifications() {
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
                Join<CustomerVehicleEntity, CustomerEntity> customerJoin = root.join("customer", JoinType.LEFT);
                Join<CustomerEntity, UserProfileEntity> userProfileJoin = customerJoin.join("userProfile", JoinType.LEFT);
                predicates.add(criteriaBuilder.or(
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("licensePlate")), normalizedKeyword),
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
}
