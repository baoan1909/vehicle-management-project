package com.ban.vehicle_management.infrastructure.mapper.ai;

import com.ban.vehicle_management.domain.ai.model.AiToolCall;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.ai.AiToolCallEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AiToolCallPersistenceMapper {

    AiToolCall toDomain(AiToolCallEntity entity);

    AiToolCallEntity toEntity(AiToolCall toolCall);
}
