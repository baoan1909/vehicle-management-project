package com.ban.vehicle_management.application.system.time.mapper;

import com.ban.vehicle_management.entrypoint.dto.system.time.response.ApplicationTimeResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ApplicationTimeApiMapper {
    ApplicationTimeResponse toResponse(String timeZone);
}
