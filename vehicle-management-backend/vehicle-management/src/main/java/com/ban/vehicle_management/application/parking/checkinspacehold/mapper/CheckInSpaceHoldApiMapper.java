package com.ban.vehicle_management.application.parking.checkinspacehold.mapper;

import com.ban.vehicle_management.application.parking.checkinspacehold.model.command.CreateCheckInSpaceHoldCommand;
import com.ban.vehicle_management.application.parking.checkinspacehold.model.result.CheckInSpaceHoldResult;
import com.ban.vehicle_management.entrypoint.dto.parking.checkinspacehold.request.CreateCheckInSpaceHoldRequest;
import com.ban.vehicle_management.entrypoint.dto.parking.checkinspacehold.response.CheckInSpaceHoldResponse;
import org.mapstruct.Mapper;
import org.springframework.http.HttpStatus;

@Mapper(componentModel = "spring")
public interface CheckInSpaceHoldApiMapper {
    CreateCheckInSpaceHoldCommand toCommand(CreateCheckInSpaceHoldRequest request);

    CheckInSpaceHoldResponse toResponse(CheckInSpaceHoldResult result);

    default HttpStatus toCreateStatus(CheckInSpaceHoldResult result) {
        return result.created() ? HttpStatus.CREATED : HttpStatus.OK;
    }

    default String toCreateMessage(CheckInSpaceHoldResult result) {
        return result.created()
                ? "Check-in parking-space hold created"
                : "Existing check-in parking-space hold fetched";
    }
}
