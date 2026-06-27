package com.ban.vehicle_management.application.operations.employeerosterrule.port.in;

import com.ban.vehicle_management.domain.operations.employeerosterrule.model.EmployeeRosterRule;
import com.ban.vehicle_management.shared.enumeration.operations.AssignmentMode;
import com.ban.vehicle_management.shared.enumeration.operations.RosterRuleStatus;
import com.ban.vehicle_management.shared.enumeration.operations.ShiftType;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface EmployeeRosterRulePortIn {

    EmployeeRosterRule createRule(EmployeeRosterRule rule);

    EmployeeRosterRule getRuleById(UUID rosterRuleId);

    List<EmployeeRosterRule> getRules(
            UUID parkingLotId,
            UUID employeeId,
            ShiftType preferredShiftType,
            UUID preferredGateId,
            DayOfWeek weeklyDayOff,
            AssignmentMode assignmentMode,
            RosterRuleStatus status,
            LocalDate effectiveDate
    );

    EmployeeRosterRule updateRule(
            UUID rosterRuleId,
            EmployeeRosterRule request
    );

    EmployeeRosterRule activateRule(UUID rosterRuleId);

    void deleteRule(UUID rosterRuleId);
}