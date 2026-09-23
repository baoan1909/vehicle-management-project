package com.ban.vehicle_management.entrypoint.controller.ai;

import com.ban.vehicle_management.application.ai.mapper.AiMessageCitationApiMapper;
import com.ban.vehicle_management.application.ai.port.in.AiMessageCitationPortIn;
import com.ban.vehicle_management.application.ai.port.out.KnowledgeRetrievalAuditPortOut;
import com.ban.vehicle_management.domain.ai.model.AiMessageCitation;
import com.ban.vehicle_management.domain.ai.model.RetrievalAudit;
import com.ban.vehicle_management.entrypoint.dto.ai.citation.response.MessageCitationsResponse;
import com.ban.vehicle_management.shared.utils.ApiResponse;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai/messages")
public class AiMessageCitationController {

    private final AiMessageCitationPortIn citationPortIn;
    private final KnowledgeRetrievalAuditPortOut auditPortOut;
    private final AiMessageCitationApiMapper mapper;

    public AiMessageCitationController(
            AiMessageCitationPortIn citationPortIn,
            KnowledgeRetrievalAuditPortOut auditPortOut,
            AiMessageCitationApiMapper mapper) {
        this.citationPortIn = citationPortIn;
        this.auditPortOut = auditPortOut;
        this.mapper = mapper;
    }

    @GetMapping("/{messageId}/citations")
    public ResponseEntity<ApiResponse<MessageCitationsResponse>> citations(@PathVariable UUID messageId) {
        List<AiMessageCitation> citations = citationPortIn.citationsForMessage(messageId);
        RetrievalAudit audit = citations.stream()
                .map(AiMessageCitation::retrievalAuditId)
                .filter(id -> id != null)
                .findFirst()
                .flatMap(auditPortOut::findById)
                .orElse(null);
        return ResponseEntity.ok(ApiResponse.ok(
                "Lấy trích dẫn tin nhắn thành công",
                mapper.toResponse(messageId, citations, audit)));
    }
}
