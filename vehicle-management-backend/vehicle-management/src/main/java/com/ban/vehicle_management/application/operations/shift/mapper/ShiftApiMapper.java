package com.ban.vehicle_management.application.operations.shift.mapper;

import com.ban.vehicle_management.domain.operations.shift.model.Shift;
import com.ban.vehicle_management.entrypoint.dto.operations.shift.response.ShiftAdminResponse;
import com.ban.vehicle_management.shared.utils.DateTimeUtils;
import java.time.Instant;
import java.util.List;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ShiftApiMapper {

    ShiftAdminResponse toAdminResponse(Shift shift);

    List<ShiftAdminResponse> toAdminResponses(
            List<Shift> shifts
    );

    default String map(Instant instant) {
        return DateTimeUtils.formatInstant(
                instant,
                DateTimeUtils.VIETNAM_ZONE
        );
    }
}