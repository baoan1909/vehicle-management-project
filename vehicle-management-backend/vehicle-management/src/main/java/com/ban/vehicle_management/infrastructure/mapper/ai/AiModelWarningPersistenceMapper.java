package com.ban.vehicle_management.infrastructure.mapper.ai;

import com.ban.vehicle_management.domain.ai.model.AiModelWarning;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.ai.AiModelWarningEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AiModelWarningPersistenceMapper {

    AiModelWarning toDomain(AiModelWarningEntity entity);

    AiModelWarningEntity toEntity(AiModelWarning warning);
}
