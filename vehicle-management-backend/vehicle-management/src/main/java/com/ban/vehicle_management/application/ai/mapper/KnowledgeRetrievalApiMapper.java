package com.ban.vehicle_management.application.ai.mapper;

import com.ban.vehicle_management.domain.ai.model.KnowledgeRetrievalResult;
import com.ban.vehicle_management.entrypoint.dto.ai.knowledge.response.KnowledgeRetrievalTestResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface KnowledgeRetrievalApiMapper {
    @Mapping(target = "results", expression = "java(com.ban.vehicle_management.domain.ai.model.KnowledgeSearchHit.fromAll(result.results()))")
    KnowledgeRetrievalTestResponse toResponse(KnowledgeRetrievalResult result);
}
