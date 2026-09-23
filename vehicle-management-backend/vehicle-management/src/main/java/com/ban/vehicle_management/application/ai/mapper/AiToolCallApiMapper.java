package com.ban.vehicle_management.application.ai.mapper;

import com.ban.vehicle_management.domain.ai.model.AiToolCall;
import com.ban.vehicle_management.entrypoint.dto.ai.toolcall.response.AiToolCallResponse;
import com.ban.vehicle_management.shared.utils.DateTimeUtils;
import java.time.Instant;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AiToolCallApiMapper {

    AiToolCallResponse toResponse(AiToolCall toolCall);

    default String map(Instant instant) {
        return DateTimeUtils.formatInstant(instant, DateTimeUtils.VIETNAM_ZONE);
    }
}
