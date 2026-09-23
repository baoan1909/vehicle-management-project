package com.ban.vehicle_management.infrastructure.mapper.ai;

import com.ban.vehicle_management.domain.ai.knowledge.model.KnowledgeSource;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.ai.KnowledgeSourceEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface KnowledgeSourcePersistenceMapper {

    KnowledgeSourceEntity toEntity(KnowledgeSource domain);

    KnowledgeSource toDomain(KnowledgeSourceEntity entity);
}