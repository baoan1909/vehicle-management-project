package com.ban.vehicle_management.infrastructure.mapper.ai;

import com.ban.vehicle_management.domain.ai.model.AiRun;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.ai.AiRunEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface AiRunPersistenceMapper {

    @Mapping(target = "fieldViolationsRedacted", defaultValue = "[]")
    AiRunEntity toEntity(AiRun domain);

    AiRun toDomain(AiRunEntity entity);
}
