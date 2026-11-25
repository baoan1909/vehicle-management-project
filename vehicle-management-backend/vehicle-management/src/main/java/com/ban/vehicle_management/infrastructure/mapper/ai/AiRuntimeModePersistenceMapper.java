package com.ban.vehicle_management.infrastructure.mapper.ai;

import com.ban.vehicle_management.domain.ai.model.AiRuntimeState;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.ai.AiRuntimeModeEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AiRuntimeModePersistenceMapper {
    AiRuntimeState toDomain(AiRuntimeModeEntity entity);
}
