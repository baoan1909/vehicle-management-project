package com.ban.vehicle_management.application.operations.employeerosterrule.port.out;

import com.ban.vehicle_management.domain.operations.employeerosterrule.model.EmployeeRosterRule;
import com.ban.vehicle_management.shared.enumeration.operations.AssignmentMode;
import com.ban.vehicle_management.shared.enumeration.operations.RosterRuleStatus;
import com.ban.vehicle_management.shared.enumeration.operations.ShiftType;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EmployeeRosterRulePortOut {

    EmployeeRosterRule save(EmployeeRosterRule rule);

    Optional<EmployeeRosterRule> findById(UUID rosterRuleId);

    List<EmployeeRosterRule> findAll(
            UUID parkingLotId,
            UUID employeeId,
            ShiftType preferredShiftType,
            UUID preferredGateId,
            DayOfWeek weeklyDayOff,
            AssignmentMode assignmentMode,
            RosterRuleStatus status,
            LocalDate effectiveDate
    );

    List<EmployeeRosterRule> findActiveByParkingLotId(
            UUID parkingLotId
    );
}