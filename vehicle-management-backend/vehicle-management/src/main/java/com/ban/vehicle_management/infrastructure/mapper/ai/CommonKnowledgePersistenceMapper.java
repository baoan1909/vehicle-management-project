package com.ban.vehicle_management.infrastructure.mapper.ai;

import com.ban.vehicle_management.domain.ai.model.CommonKnowledgeCandidate;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.ai.KnowledgeChunkRepository;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface CommonKnowledgePersistenceMapper {
    CommonKnowledgeCandidate toDomain(KnowledgeChunkRepository.CommonProjection projection);
}
