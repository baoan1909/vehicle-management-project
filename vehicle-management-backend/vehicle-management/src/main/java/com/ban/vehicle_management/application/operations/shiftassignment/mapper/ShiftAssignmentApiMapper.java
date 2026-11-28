package com.ban.vehicle_management.application.operations.shiftassignment.mapper;

import com.ban.vehicle_management.domain.operations.shiftassignment.model.EmployeeShiftSchedule;
import com.ban.vehicle_management.entrypoint.dto.operations.shiftassignment.response.EmployeeShiftScheduleResponse;

import com.ban.vehicle_management.domain.operations.shiftassignment.model.ShiftAssignment;
import com.ban.vehicle_management.entrypoint.dto.operations.shiftassignment.request.CreateShiftAssignmentRequest;
import com.ban.vehicle_management.entrypoint.dto.operations.shiftassignment.request.UpdateShiftAssignmentRequest;
import com.ban.vehicle_management.entrypoint.dto.operations.shiftassignment.response.ShiftAssignmentAdminResponse;
import com.ban.vehicle_management.shared.utils.DateTimeUtils;
import java.time.Instant;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ShiftAssignmentApiMapper {
    @Mapping(target = "shiftAssignmentId", source = "assignment.shiftAssignmentId")
    @Mapping(target = "shiftId", source = "shift.shiftId")
    @Mapping(target = "parkingLotId", source = "shift.parkingLotId")
    @Mapping(target = "shiftCode", source = "shift.shiftCode")
    @Mapping(target = "startTime", source = "shift.startTime")
    @Mapping(target = "endTime", source = "shift.endTime")
    @Mapping(target = "shiftStatus", source = "shift.status")
    @Mapping(target = "status", source = "assignment.status")
    EmployeeShiftScheduleResponse toOwnResponse(
            EmployeeShiftSchedule schedule);

    List<EmployeeShiftScheduleResponse> toOwnResponses(
            List<EmployeeShiftSchedule> schedules);


    @Mapping(target = "shiftAssignmentId", ignore = true)
    @Mapping(target = "shiftId", ignore = true)
    @Mapping(target = "employeeParkingLotAssignmentId", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    ShiftAssignment toDomain(
            CreateShiftAssignmentRequest request
    );

    @Mapping(target = "shiftAssignmentId", ignore = true)
    @Mapping(target = "shiftId", ignore = true)
    @Mapping(target = "employeeParkingLotAssignmentId", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    ShiftAssignment toDomain(
            UpdateShiftAssignmentRequest request
    );

    ShiftAssignmentAdminResponse toAdminResponse(
            ShiftAssignment assignment
    );

    List<ShiftAssignmentAdminResponse> toAdminResponses(
            List<ShiftAssignment> assignments
    );

}
