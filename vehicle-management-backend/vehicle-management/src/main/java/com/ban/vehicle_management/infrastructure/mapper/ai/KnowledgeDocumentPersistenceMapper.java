package com.ban.vehicle_management.infrastructure.mapper.ai;

import com.ban.vehicle_management.domain.ai.knowledge.model.KnowledgeDocument;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.ai.KnowledgeDocumentEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface KnowledgeDocumentPersistenceMapper {

    KnowledgeDocumentEntity toEntity(KnowledgeDocument domain);

    KnowledgeDocument toDomain(KnowledgeDocumentEntity entity);
}