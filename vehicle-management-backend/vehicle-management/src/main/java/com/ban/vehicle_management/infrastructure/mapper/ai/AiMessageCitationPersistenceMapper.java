package com.ban.vehicle_management.infrastructure.mapper.ai;

import com.ban.vehicle_management.domain.ai.model.AiMessageCitation;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.ai.AiMessageCitationEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface AiMessageCitationPersistenceMapper {

    @Mapping(target = "createdAt", ignore = true)
    AiMessageCitationEntity toEntity(AiMessageCitation domain);

    AiMessageCitation toDomain(AiMessageCitationEntity entity);
}
