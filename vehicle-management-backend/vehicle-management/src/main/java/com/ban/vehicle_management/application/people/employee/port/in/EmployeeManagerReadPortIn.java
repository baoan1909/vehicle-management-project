package com.ban.vehicle_management.application.people.employee.port.in;

import com.ban.vehicle_management.application.people.employee.model.result.EmployeeActivityTimelineResult;
import com.ban.vehicle_management.application.people.employee.model.result.EmployeeRecentShiftResult;
import java.util.List;
import java.util.UUID;

public interface EmployeeManagerReadPortIn {

    List<EmployeeRecentShiftResult> getRecentShifts(
            UUID employeeId,
            Integer limit
    );

    List<EmployeeActivityTimelineResult> getActivityTimeline(
            UUID employeeId,
            Integer limit
    );
}
