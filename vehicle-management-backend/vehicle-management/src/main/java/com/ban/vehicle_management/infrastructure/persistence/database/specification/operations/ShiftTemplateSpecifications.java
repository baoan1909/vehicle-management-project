package com.ban.vehicle_management.infrastructure.persistence.database.specification.operations;

import com.ban.vehicle_management.infrastructure.persistence.database.entity.operations.ShiftTemplateEntity;
import com.ban.vehicle_management.shared.enumeration.operations.ShiftTemplateStatus;
import com.ban.vehicle_management.shared.enumeration.operations.ShiftType;
import org.springframework.data.jpa.domain.Specification;

import java.util.Locale;
import java.util.UUID;

public final class ShiftTemplateSpecifications {

    private ShiftTemplateSpecifications() {
    }

    public static Specification<ShiftTemplateEntity> withFilters(
            UUID parkingLotId,
            ShiftType shiftType,
            ShiftTemplateStatus status,
            String keyword
    ) {
        return Specification
                .where(hasParkingLotId(parkingLotId))
                .and(hasShiftType(shiftType))
                .and(hasStatus(status))
                .and(containsKeyword(keyword));
    }

    private static Specification<ShiftTemplateEntity> hasParkingLotId(UUID parkingLotId) {
        return (root, query, criteriaBuilder) ->
                parkingLotId == null
                        ? null
                        : criteriaBuilder.equal(root.get("parkingLotId"), parkingLotId);
    }

    private static Specification<ShiftTemplateEntity> hasShiftType(ShiftType shiftType) {
        return (root, query, criteriaBuilder) ->
                shiftType == null
                        ? null
                        : criteriaBuilder.equal(root.get("shiftType"), shiftType);
    }

    private static Specification<ShiftTemplateEntity> hasStatus(
            ShiftTemplateStatus status
    ) {
        return (root, query, criteriaBuilder) ->
                status == null
                        ? null
                        : criteriaBuilder.equal(root.get("status"), status);
    }

    private static Specification<ShiftTemplateEntity> containsKeyword(String keyword) {
        return (root, query, criteriaBuilder) -> {
            if (keyword == null || keyword.isBlank()) {
                return null;
            }

            String pattern = "%"
                    + keyword.trim().toLowerCase(Locale.ROOT)
                    + "%";

            return criteriaBuilder.like(
                    criteriaBuilder.lower(root.get("name")),
                    pattern
            );
        };
    }
}