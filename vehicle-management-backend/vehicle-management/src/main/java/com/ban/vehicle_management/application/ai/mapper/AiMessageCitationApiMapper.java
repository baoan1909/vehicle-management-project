package com.ban.vehicle_management.application.ai.mapper;

import com.ban.vehicle_management.application.ai.query.MessageCitationsResult;
import com.ban.vehicle_management.domain.ai.model.AiMessageCitation;
import com.ban.vehicle_management.entrypoint.dto.ai.citation.response.AiMessageCitationResponse;
import com.ban.vehicle_management.entrypoint.dto.ai.citation.response.MessageCitationsResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AiMessageCitationApiMapper {

    MessageCitationsResponse toResponse(MessageCitationsResult result);

    AiMessageCitationResponse toResponse(AiMessageCitation citation);
}
