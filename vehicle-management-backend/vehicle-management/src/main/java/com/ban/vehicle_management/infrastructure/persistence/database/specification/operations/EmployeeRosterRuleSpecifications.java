package com.ban.vehicle_management.infrastructure.persistence.database.specification.operations;

import com.ban.vehicle_management.infrastructure.persistence.database.entity.operations.EmployeeRosterRuleEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.people.EmployeeEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.people.UserProfileEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.iam.AccountEntity;
import com.ban.vehicle_management.shared.enumeration.people.EmployeeStatus;
import com.ban.vehicle_management.shared.enumeration.people.UserProfileStatus;
import com.ban.vehicle_management.shared.enumeration.iam.AccountStatus;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import com.ban.vehicle_management.shared.enumeration.operations.AssignmentMode;
import com.ban.vehicle_management.shared.enumeration.operations.RosterRuleStatus;
import com.ban.vehicle_management.shared.enumeration.operations.ShiftType;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;

public final class EmployeeRosterRuleSpecifications {

    private EmployeeRosterRuleSpecifications() {
    }

    public static Specification<EmployeeRosterRuleEntity> withFilters(
            UUID parkingLotId,
            UUID employeeId,
            ShiftType preferredShiftType,
            UUID preferredGateId,
            DayOfWeek weeklyDayOff,
            AssignmentMode assignmentMode,
            RosterRuleStatus status,
            LocalDate effectiveDate
    ) {
        return Specification
                .where(businessVisibleEmployee())
                .and(hasParkingLotId(parkingLotId))
                .and(hasEmployeeId(employeeId))
                .and(hasPreferredShiftType(preferredShiftType))
                .and(hasPreferredGateId(preferredGateId))
                .and(hasWeeklyDayOff(weeklyDayOff))
                .and(hasAssignmentMode(assignmentMode))
                .and(hasStatus(status))
                .and(isEffectiveOn(effectiveDate));
    }

    private static Specification<EmployeeRosterRuleEntity> businessVisibleEmployee() {
        return (root, query, cb) -> {
            Join<EmployeeRosterRuleEntity, EmployeeEntity> employee = root.join("employee", JoinType.INNER);
            Join<EmployeeEntity, UserProfileEntity> profile = employee.join("userProfile", JoinType.INNER);
            Join<UserProfileEntity, AccountEntity> account = profile.join("account", JoinType.INNER);
            return cb.and(
                    cb.notEqual(employee.get("status"), EmployeeStatus.DELETED),
                    cb.notEqual(profile.get("status"), UserProfileStatus.DELETED),
                    cb.not(account.get("status").in(AccountStatus.PENDING_DELETION, AccountStatus.DELETED))
            );
        };
    }

    private static Specification<EmployeeRosterRuleEntity> hasParkingLotId(
            UUID parkingLotId
    ) {
        return (root, query, cb) ->
                parkingLotId == null
                        ? null
                        : cb.equal(root.get("parkingLotId"), parkingLotId);
    }

    private static Specification<EmployeeRosterRuleEntity> hasEmployeeId(
            UUID employeeId
    ) {
        return (root, query, cb) ->
                employeeId == null
                        ? null
                        : cb.equal(root.get("employeeId"), employeeId);
    }

    private static Specification<EmployeeRosterRuleEntity> hasPreferredShiftType(
            ShiftType preferredShiftType
    ) {
        return (root, query, cb) ->
                preferredShiftType == null
                        ? null
                        : cb.equal(
                        root.get("preferredShiftType"),
                        preferredShiftType
                );
    }

    private static Specification<EmployeeRosterRuleEntity> hasPreferredGateId(
            UUID preferredGateId
    ) {
        return (root, query, cb) ->
                preferredGateId == null
                        ? null
                        : cb.equal(
                        root.get("preferredGateId"),
                        preferredGateId
                );
    }

    private static Specification<EmployeeRosterRuleEntity> hasWeeklyDayOff(
            DayOfWeek weeklyDayOff
    ) {
        return (root, query, cb) ->
                weeklyDayOff == null
                        ? null
                        : cb.equal(
                        root.get("weeklyDayOff"),
                        weeklyDayOff
                );
    }

    private static Specification<EmployeeRosterRuleEntity> hasAssignmentMode(
            AssignmentMode assignmentMode
    ) {
        return (root, query, cb) ->
                assignmentMode == null
                        ? null
                        : cb.equal(
                        root.get("assignmentMode"),
                        assignmentMode
                );
    }

    private static Specification<EmployeeRosterRuleEntity> hasStatus(
            RosterRuleStatus status
    ) {
        return (root, query, cb) ->
                status == null
                        ? null
                        : cb.equal(root.get("status"), status);
    }

    private static Specification<EmployeeRosterRuleEntity> isEffectiveOn(
            LocalDate effectiveDate
    ) {
        return (root, query, cb) -> {
            if (effectiveDate == null) {
                return null;
            }

            return cb.and(
                    cb.lessThanOrEqualTo(
                            root.get("effectiveFrom"),
                            effectiveDate
                    ),
                    cb.or(
                            cb.isNull(root.get("effectiveTo")),
                            cb.greaterThanOrEqualTo(
                                    root.get("effectiveTo"),
                                    effectiveDate
                            )
                    )
            );
        };
    }
}
