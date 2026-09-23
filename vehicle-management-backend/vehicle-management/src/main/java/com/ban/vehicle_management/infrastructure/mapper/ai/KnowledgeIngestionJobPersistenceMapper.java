package com.ban.vehicle_management.infrastructure.mapper.ai;

import com.ban.vehicle_management.domain.ai.knowledge.model.KnowledgeIngestionJob;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.ai.KnowledgeIngestionJobEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface KnowledgeIngestionJobPersistenceMapper {

    KnowledgeIngestionJobEntity toEntity(KnowledgeIngestionJob domain);

    KnowledgeIngestionJob toDomain(KnowledgeIngestionJobEntity entity);
}