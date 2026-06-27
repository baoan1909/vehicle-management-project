package com.ban.vehicle_management.infrastructure.persistence.database.specification.operations;

import com.ban.vehicle_management.infrastructure.persistence.database.entity.operations.ShiftAssignmentEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.operations.ShiftEntity;
import com.ban.vehicle_management.shared.enumeration.operations.ShiftAssignmentStatus;
import com.ban.vehicle_management.shared.enumeration.operations.ShiftStatus;
import com.ban.vehicle_management.shared.enumeration.operations.ShiftType;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import java.time.LocalDate;
import java.util.Locale;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;

public final class ShiftSpecifications {

    private ShiftSpecifications() {
    }

    public static Specification<ShiftEntity> withFilters(
            UUID parkingLotId,
            LocalDate fromDate,
            LocalDate toDate,
            ShiftType shiftType,
            ShiftStatus status,
            UUID employeeId,
            String keyword
    ) {
        return Specification
                .where(hasParkingLotId(parkingLotId))
                .and(fromDate(fromDate))
                .and(toDate(toDate))
                .and(hasShiftType(shiftType))
                .and(hasStatus(status))
                .and(hasActiveEmployee(employeeId))
                .and(containsKeyword(keyword));
    }

    private static Specification<ShiftEntity> hasParkingLotId(
            UUID parkingLotId
    ) {
        return (root, query, cb) ->
                parkingLotId == null
                        ? null
                        : cb.equal(
                        root.get("parkingLotId"),
                        parkingLotId
                );
    }

    private static Specification<ShiftEntity> fromDate(
            LocalDate fromDate
    ) {
        return (root, query, cb) ->
                fromDate == null
                        ? null
                        : cb.greaterThanOrEqualTo(
                        root.get("shiftDate"),
                        fromDate
                );
    }

    private static Specification<ShiftEntity> toDate(
            LocalDate toDate
    ) {
        return (root, query, cb) ->
                toDate == null
                        ? null
                        : cb.lessThanOrEqualTo(
                        root.get("shiftDate"),
                        toDate
                );
    }

    private static Specification<ShiftEntity> hasShiftType(
            ShiftType shiftType
    ) {
        return (root, query, cb) ->
                shiftType == null
                        ? null
                        : cb.equal(root.get("shiftType"), shiftType);
    }

    private static Specification<ShiftEntity> hasStatus(
            ShiftStatus status
    ) {
        return (root, query, cb) ->
                status == null
                        ? null
                        : cb.equal(root.get("status"), status);
    }

    private static Specification<ShiftEntity> hasActiveEmployee(
            UUID employeeId
    ) {
        return (root, query, cb) -> {
            if (employeeId == null) {
                return null;
            }

            query.distinct(true);

            Join<ShiftEntity, ShiftAssignmentEntity> assignment =
                    root.join("shiftAssignments", JoinType.INNER);

            return cb.and(
                    cb.equal(
                            assignment.get("employeeId"),
                            employeeId
                    ),
                    cb.equal(
                            assignment.get("status"),
                            ShiftAssignmentStatus.ACTIVE
                    )
            );
        };
    }

    private static Specification<ShiftEntity> containsKeyword(
            String keyword
    ) {
        return (root, query, cb) -> {
            if (keyword == null || keyword.isBlank()) {
                return null;
            }

            String pattern = "%"
                    + keyword.trim().toLowerCase(Locale.ROOT)
                    + "%";

            return cb.or(
                    cb.like(
                            cb.lower(root.get("shiftCode")),
                            pattern
                    ),
                    cb.like(
                            cb.lower(root.get("note")),
                            pattern
                    )
            );
        };
    }
}