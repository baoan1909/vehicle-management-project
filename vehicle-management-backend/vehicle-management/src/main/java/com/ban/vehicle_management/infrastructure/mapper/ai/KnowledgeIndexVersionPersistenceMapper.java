package com.ban.vehicle_management.infrastructure.mapper.ai;

import com.ban.vehicle_management.domain.ai.model.KnowledgeIndexVersion;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.ai.KnowledgeIndexVersionEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface KnowledgeIndexVersionPersistenceMapper {

    KnowledgeIndexVersion toDomain(KnowledgeIndexVersionEntity entity);

    KnowledgeIndexVersionEntity toEntity(KnowledgeIndexVersion domain);
}