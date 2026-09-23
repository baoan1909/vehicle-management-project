package com.ban.vehicle_management.infrastructure.mapper.ai;

import com.ban.vehicle_management.domain.ai.model.AssistantJob;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.ai.AssistantJobEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AssistantJobPersistenceMapper {

    AssistantJobEntity toEntity(AssistantJob domain);

    AssistantJob toDomain(AssistantJobEntity entity);
}
