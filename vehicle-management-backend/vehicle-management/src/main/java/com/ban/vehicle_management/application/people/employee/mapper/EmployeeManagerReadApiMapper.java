package com.ban.vehicle_management.application.people.employee.mapper;

import com.ban.vehicle_management.application.people.employee.model.result.EmployeeActivityTimelineResult;
import com.ban.vehicle_management.application.people.employee.model.result.EmployeeRecentShiftResult;
import com.ban.vehicle_management.entrypoint.dto.people.employee.response.EmployeeActivityTimelineResponse;
import com.ban.vehicle_management.entrypoint.dto.people.employee.response.EmployeeRecentShiftResponse;
import java.util.List;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface EmployeeManagerReadApiMapper {

    EmployeeRecentShiftResponse toRecentShiftResponse(EmployeeRecentShiftResult result);

    List<EmployeeRecentShiftResponse> toRecentShiftResponses(List<EmployeeRecentShiftResult> results);

    EmployeeActivityTimelineResponse toActivityTimelineResponse(EmployeeActivityTimelineResult result);

    List<EmployeeActivityTimelineResponse> toActivityTimelineResponses(List<EmployeeActivityTimelineResult> results);
}
