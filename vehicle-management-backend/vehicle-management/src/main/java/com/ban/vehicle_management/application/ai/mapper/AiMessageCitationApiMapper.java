package com.ban.vehicle_management.application.ai.mapper;

import com.ban.vehicle_management.domain.ai.model.AiMessageCitation;
import com.ban.vehicle_management.domain.ai.model.RetrievalAudit;
import com.ban.vehicle_management.entrypoint.dto.ai.citation.response.AiMessageCitationResponse;
import com.ban.vehicle_management.entrypoint.dto.ai.citation.response.MessageCitationsResponse;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AiMessageCitationApiMapper {

    default MessageCitationsResponse toResponse(
            UUID messageId, List<AiMessageCitation> citations, RetrievalAudit audit) {
        MessageCitationsResponse response = new MessageCitationsResponse();
        response.setMessageId(messageId);
        response.setCitations(citations.stream().map(this::toResponse).toList());
        if (audit != null && audit.groundedConfidence() != null) {
            response.setGroundedConfidence(audit.groundedConfidence());
            response.setHandoffRecommended(audit.groundedConfidence().compareTo(new BigDecimal("0.55")) < 0
                    || "INSUFFICIENT_EVIDENCE".equals(audit.diagnosticCode()));
            response.setDiagnosticCode(audit.diagnosticCode());
        } else {
            response.setGroundedConfidence(null);
            response.setHandoffRecommended(false);
            response.setDiagnosticCode(null);
        }
        return response;
    }

    default AiMessageCitationResponse toResponse(AiMessageCitation citation) {
        AiMessageCitationResponse response = new AiMessageCitationResponse();
        response.setCitationId(citation.citationId());
        response.setMessageId(citation.messageId());
        response.setDocumentId(citation.documentId());
        response.setChunkId(citation.chunkId());
        response.setLabel(citation.label());
        response.setTitle(citation.title());
        response.setSourcePage(citation.sourcePage());
        response.setSourceSection(citation.sourceSection());
        response.setRetrievalScore(citation.retrievalScore());
        response.setRetrievalAuditId(citation.retrievalAuditId());
        response.setIndexVersionId(citation.indexVersionId());
        response.setCitationOrder(citation.citationOrder());
        return response;
    }
}
