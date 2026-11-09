package com.ban.vehicle_management.application.ai.mapper;

import com.ban.vehicle_management.application.ai.port.in.AssistantStatusPortIn;
import com.ban.vehicle_management.entrypoint.dto.ai.assistant.response.AssistantMessageStatusResponse;
import com.ban.vehicle_management.entrypoint.dto.ai.assistant.response.AssistantStatusResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AssistantApiMapper {
    AssistantStatusResponse toStatusResponse(Boolean enabled);
    AssistantMessageStatusResponse toMessageStatusResponse(AssistantStatusPortIn.MessageStatus status);
}
