package com.ban.vehicle_management.application.ai.mapper;

import com.ban.vehicle_management.domain.ai.model.KnowledgeSearchHit;
import com.ban.vehicle_management.domain.ai.model.KnowledgeRetrievalResult;
import com.ban.vehicle_management.entrypoint.dto.ai.knowledge.response.KnowledgeRetrievalTestResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface KnowledgeRetrievalApiMapper {

    default KnowledgeRetrievalTestResponse toResponse(KnowledgeRetrievalResult result) {
        KnowledgeRetrievalTestResponse response = new KnowledgeRetrievalTestResponse();
        response.setDiagnosticCode(result.diagnosticCode());
        response.setActiveIndexVersionId(result.activeIndexVersionId());
        response.setResults(KnowledgeSearchHit.fromAll(result.results()));
        response.setRetrievalAuditId(result.retrievalAuditId());
        response.setGroundedConfidence(result.groundedConfidence());
        response.setEvidenceSufficient(result.evidenceSufficient());
        response.setNormalizedQuery(result.normalizedQuery());
        response.setRetrievalPolicyVersion(result.retrievalPolicyVersion());
        return response;
    }
}