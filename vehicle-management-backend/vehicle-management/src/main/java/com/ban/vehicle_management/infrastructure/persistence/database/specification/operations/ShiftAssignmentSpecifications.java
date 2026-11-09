package com.ban.vehicle_management.infrastructure.persistence.database.specification.operations;

import com.ban.vehicle_management.infrastructure.persistence.database.entity.operations.ShiftAssignmentEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.operations.ShiftEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.people.EmployeeEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.people.UserProfileEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.iam.AccountEntity;
import com.ban.vehicle_management.shared.enumeration.people.EmployeeStatus;
import com.ban.vehicle_management.shared.enumeration.people.UserProfileStatus;
import com.ban.vehicle_management.shared.enumeration.iam.AccountStatus;
import com.ban.vehicle_management.shared.enumeration.operations.ShiftAssignmentStatus;
import com.ban.vehicle_management.shared.enumeration.operations.ShiftType;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;

public final class ShiftAssignmentSpecifications {

    private ShiftAssignmentSpecifications() {
    }

    public static Specification<ShiftAssignmentEntity> withFilters(
            UUID parkingLotId,
            UUID shiftId,
            UUID employeeId,
            UUID gateId,
            ShiftAssignmentStatus status,
            LocalDate fromDate,
            LocalDate toDate,
            ShiftType shiftType
    ) {
        return Specification
                .where(businessVisibleEmployee())
                .and(hasShiftId(shiftId))
                .and(hasEmployeeId(employeeId))
                .and(hasGateId(gateId))
                .and(hasStatus(status))
                .and(matchesShift(
                        parkingLotId,
                        fromDate,
                        toDate,
                        shiftType
                ));
    }

    private static Specification<ShiftAssignmentEntity> businessVisibleEmployee() {
        return (root, query, cb) -> {
            Join<ShiftAssignmentEntity, EmployeeEntity> employee = root.join("employee", JoinType.INNER);
            Join<EmployeeEntity, UserProfileEntity> profile = employee.join("userProfile", JoinType.INNER);
            Join<UserProfileEntity, AccountEntity> account = profile.join("account", JoinType.INNER);
            return cb.and(
                    cb.notEqual(employee.get("status"), EmployeeStatus.DELETED),
                    cb.notEqual(profile.get("status"), UserProfileStatus.DELETED),
                    cb.not(account.get("status").in(AccountStatus.PENDING_DELETION, AccountStatus.DELETED))
            );
        };
    }

    private static Specification<ShiftAssignmentEntity> hasShiftId(
            UUID shiftId
    ) {
        return (root, query, cb) -> shiftId == null
                ? null
                : cb.equal(root.get("shiftId"), shiftId);
    }

    private static Specification<ShiftAssignmentEntity> hasEmployeeId(
            UUID employeeId
    ) {
        return (root, query, cb) -> employeeId == null
                ? null
                : cb.equal(root.get("employeeId"), employeeId);
    }

    private static Specification<ShiftAssignmentEntity> hasGateId(
            UUID gateId
    ) {
        return (root, query, cb) -> gateId == null
                ? null
                : cb.equal(root.get("gateId"), gateId);
    }

    private static Specification<ShiftAssignmentEntity> hasStatus(
            ShiftAssignmentStatus status
    ) {
        return (root, query, cb) -> status == null
                ? null
                : cb.equal(root.get("status"), status);
    }

    private static Specification<ShiftAssignmentEntity> matchesShift(
            UUID parkingLotId,
            LocalDate fromDate,
            LocalDate toDate,
            ShiftType shiftType
    ) {
        return (root, query, cb) -> {
            if (parkingLotId == null
                    && fromDate == null
                    && toDate == null
                    && shiftType == null) {
                return null;
            }

            Join<ShiftAssignmentEntity, ShiftEntity> shift =
                    root.join("shift", JoinType.INNER);

            List<Predicate> predicates = new ArrayList<>();

            if (parkingLotId != null) {
                predicates.add(cb.equal(
                        shift.get("parkingLotId"),
                        parkingLotId
                ));
            }

            if (fromDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(
                        shift.get("shiftDate"),
                        fromDate
                ));
            }

            if (toDate != null) {
                predicates.add(cb.lessThanOrEqualTo(
                        shift.get("shiftDate"),
                        toDate
                ));
            }

            if (shiftType != null) {
                predicates.add(cb.equal(
                        shift.get("shiftType"),
                        shiftType
                ));
            }

            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }
}
