package com.ban.vehicle_management.application.ai.mapper;

import com.ban.vehicle_management.domain.ai.model.AiRuntimeState;
import com.ban.vehicle_management.entrypoint.dto.ai.model.response.AiRuntimeModeResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AiRuntimeApiMapper {
    AiRuntimeModeResponse toResponse(AiRuntimeState state);
}
