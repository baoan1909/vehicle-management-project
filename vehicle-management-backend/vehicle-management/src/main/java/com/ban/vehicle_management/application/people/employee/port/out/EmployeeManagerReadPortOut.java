package com.ban.vehicle_management.application.people.employee.port.out;

import com.ban.vehicle_management.application.people.employee.model.result.EmployeeActivityTimelineResult;
import com.ban.vehicle_management.application.people.employee.model.result.EmployeeRecentShiftResult;
import java.util.List;
import java.util.UUID;

public interface EmployeeManagerReadPortOut {

    List<EmployeeRecentShiftResult> findRecentShifts(
            UUID employeeId,
            int limit
    );

    List<EmployeeActivityTimelineResult> findActivityTimeline(
            UUID employeeId,
            int limit
    );
}
