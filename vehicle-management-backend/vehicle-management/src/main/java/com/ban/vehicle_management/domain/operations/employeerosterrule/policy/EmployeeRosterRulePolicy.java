package com.ban.vehicle_management.domain.operations.employeerosterrule.policy;

import com.ban.vehicle_management.domain.operations.employeerosterrule.model.EmployeeRosterRule;
import com.ban.vehicle_management.shared.enumeration.operations.AssignmentMode;
import com.ban.vehicle_management.shared.enumeration.operations.RosterRuleStatus;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import com.ban.vehicle_management.shared.exception.ConflictException;
import java.time.LocalDate;

public class EmployeeRosterRulePolicy {

    public void initialize(EmployeeRosterRule rule) {
        requireRule(rule);

        if (rule.getStatus() == null) {
            rule.setStatus(RosterRuleStatus.ACTIVE);
        }

        validateState(rule);
    }

    public void validateState(EmployeeRosterRule rule) {
        requireRule(rule);

        requireField(rule.getParkingLotId(), "parkingLotId");
        requireField(rule.getEmployeeId(), "employeeId");
        requireField(rule.getWeeklyDayOff(), "weeklyDayOff");
        requireField(rule.getAssignmentMode(), "assignmentMode");
        requireField(rule.getEffectiveFrom(), "effectiveFrom");
        requireField(rule.getStatus(), "status");

        validateEffectivePeriod(rule);
        validateAssignmentModeFields(rule);
    }

    public void activate(EmployeeRosterRule rule, LocalDate currentDate) {
        requireRule(rule);
        requireField(currentDate, "currentDate");

        validateState(rule);

        if (rule.getEffectiveTo() != null
                && rule.getEffectiveTo().isBefore(currentDate)) {
            throw new ConflictException(
                    "Cannot activate an expired employee roster rule"
            );
        }

        rule.setStatus(RosterRuleStatus.ACTIVE);
    }

    public void deactivate(EmployeeRosterRule rule) {
        requireRule(rule);
        rule.setStatus(RosterRuleStatus.INACTIVE);
        validateState(rule);
    }

    public boolean periodsOverlap(
            EmployeeRosterRule first,
            EmployeeRosterRule second
    ) {
        LocalDate firstEnd = first.getEffectiveTo();
        LocalDate secondEnd = second.getEffectiveTo();

        boolean firstStartsBeforeSecondEnds =
                secondEnd == null
                        || !first.getEffectiveFrom().isAfter(secondEnd);

        boolean secondStartsBeforeFirstEnds =
                firstEnd == null
                        || !second.getEffectiveFrom().isAfter(firstEnd);

        return firstStartsBeforeSecondEnds
                && secondStartsBeforeFirstEnds;
    }

    public boolean isEffectiveOn(
            EmployeeRosterRule rule,
            LocalDate effectiveDate
    ) {
        requireRule(rule);
        requireField(effectiveDate, "effectiveDate");

        boolean started =
                !effectiveDate.isBefore(rule.getEffectiveFrom());

        boolean notEnded =
                rule.getEffectiveTo() == null
                        || !effectiveDate.isAfter(rule.getEffectiveTo());

        return started && notEnded;
    }

    private void validateEffectivePeriod(EmployeeRosterRule rule) {
        if (rule.getEffectiveTo() != null
                && rule.getEffectiveTo().isBefore(
                rule.getEffectiveFrom()
        )) {
            throw new BadRequestException(
                    "effectiveTo must not be before effectiveFrom"
            );
        }
    }

    private void validateAssignmentModeFields(
            EmployeeRosterRule rule
    ) {
        if (rule.getAssignmentMode() == AssignmentMode.FIXED) {
            if (rule.getPreferredShiftType() == null) {
                throw new BadRequestException(
                        "preferredShiftType is required for FIXED rule"
                );
            }

            if (rule.getPreferredGateId() == null) {
                throw new BadRequestException(
                        "preferredGateId is required for FIXED rule"
                );
            }

            return;
        }

        if (rule.getAssignmentMode() == AssignmentMode.RELIEF) {
            if (rule.getPreferredShiftType() != null) {
                throw new BadRequestException(
                        "preferredShiftType must be null for RELIEF rule"
                );
            }

            if (rule.getPreferredGateId() != null) {
                throw new BadRequestException(
                        "preferredGateId must be null for RELIEF rule"
                );
            }
        }
    }

    private void requireRule(EmployeeRosterRule rule) {
        requireField(rule, "employeeRosterRule");
    }

    private void requireField(Object value, String fieldName) {
        if (value == null) {
            throw new BadRequestException(
                    fieldName + " must not be null"
            );
        }
    }
}