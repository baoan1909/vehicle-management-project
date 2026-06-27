package com.ban.vehicle_management.entrypoint.dto.operations.employeerosterrule.request;

import com.ban.vehicle_management.shared.enumeration.operations.AssignmentMode;
import com.ban.vehicle_management.shared.enumeration.operations.ShiftType;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.UUID;

public record CreateEmployeeRosterRuleRequest(
        UUID parkingLotId,
        UUID employeeId,
        ShiftType preferredShiftType,
        UUID preferredGateId,
        DayOfWeek weeklyDayOff,
        AssignmentMode assignmentMode,
        LocalDate effectiveFrom,
        LocalDate effectiveTo
) {
}