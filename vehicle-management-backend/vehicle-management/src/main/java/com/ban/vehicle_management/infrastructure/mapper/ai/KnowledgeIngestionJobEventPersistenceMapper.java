package com.ban.vehicle_management.infrastructure.mapper.ai;

import com.ban.vehicle_management.domain.ai.knowledge.model.KnowledgeIngestionJobEvent;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.ai.KnowledgeIngestionJobEventEntity;
import java.util.List;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface KnowledgeIngestionJobEventPersistenceMapper {

    KnowledgeIngestionJobEventEntity toEntity(KnowledgeIngestionJobEvent domain);

    KnowledgeIngestionJobEvent toDomain(KnowledgeIngestionJobEventEntity entity);

    List<KnowledgeIngestionJobEvent> toDomainList(List<KnowledgeIngestionJobEventEntity> entities);
}