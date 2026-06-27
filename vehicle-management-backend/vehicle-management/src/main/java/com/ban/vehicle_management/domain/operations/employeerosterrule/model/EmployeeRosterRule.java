package com.ban.vehicle_management.domain.operations.employeerosterrule.model;

import com.ban.vehicle_management.domain.common.model.AuditableDomainModel;
import com.ban.vehicle_management.shared.enumeration.operations.AssignmentMode;
import com.ban.vehicle_management.shared.enumeration.operations.RosterRuleStatus;
import com.ban.vehicle_management.shared.enumeration.operations.ShiftType;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeRosterRule extends AuditableDomainModel {

    private UUID rosterRuleId;
    private UUID parkingLotId;
    private UUID employeeId;
    private ShiftType preferredShiftType;
    private UUID preferredGateId;
    private DayOfWeek weeklyDayOff;
    private AssignmentMode assignmentMode;
    private LocalDate effectiveFrom;
    private LocalDate effectiveTo;
    private RosterRuleStatus status;
}