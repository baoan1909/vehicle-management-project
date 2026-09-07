package com.ban.vehicle_management.infrastructure.mapper.ai;

import com.ban.vehicle_management.domain.ai.model.AiModelConfiguration;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.ai.AiModelConfigurationEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AiModelConfigurationPersistenceMapper {

    AiModelConfiguration toDomain(AiModelConfigurationEntity entity);
}
