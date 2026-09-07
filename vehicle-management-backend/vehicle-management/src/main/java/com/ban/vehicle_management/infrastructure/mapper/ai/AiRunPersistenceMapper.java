package com.ban.vehicle_management.infrastructure.mapper.ai;

import com.ban.vehicle_management.domain.ai.model.AiRun;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.ai.AiRunEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AiRunPersistenceMapper {

    AiRunEntity toEntity(AiRun domain);

    AiRun toDomain(AiRunEntity entity);
}
