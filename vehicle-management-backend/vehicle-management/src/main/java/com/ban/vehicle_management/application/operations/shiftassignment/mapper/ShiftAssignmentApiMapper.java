package com.ban.vehicle_management.application.operations.shiftassignment.mapper;

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

    @Mapping(target = "shiftAssignmentId", ignore = true)
    @Mapping(target = "shiftId", ignore = true)
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

    default String map(Instant instant) {
        return DateTimeUtils.formatInstant(
                instant,
                DateTimeUtils.VIETNAM_ZONE
        );
    }
}