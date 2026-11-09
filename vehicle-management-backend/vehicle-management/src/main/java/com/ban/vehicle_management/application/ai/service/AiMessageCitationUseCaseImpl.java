package com.ban.vehicle_management.application.ai.service;

import com.ban.vehicle_management.application.ai.port.in.AiMessageCitationPortIn;
import com.ban.vehicle_management.application.ai.port.out.AiMessageCitationPortOut;
import com.ban.vehicle_management.application.ai.port.out.KnowledgeRetrievalAuditPortOut;
import com.ban.vehicle_management.application.ai.query.MessageCitationsResult;
import com.ban.vehicle_management.application.iam.account.port.in.CurrentAccountPortIn;
import com.ban.vehicle_management.application.operations.chatconversation.port.out.ChatConversationPortOut;
import com.ban.vehicle_management.domain.ai.model.AiMessageCitation;
import com.ban.vehicle_management.domain.ai.model.RetrievalAudit;
import com.ban.vehicle_management.domain.operations.chatmessage.model.ChatMessage;
import com.ban.vehicle_management.shared.exception.NotFoundException;
import java.util.List;
import java.util.UUID;
import java.math.BigDecimal;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Serves persisted citations with conversation-membership authorization: a
 * caller sees citations only for messages in conversations they belong to.
 */
@Service
public class AiMessageCitationUseCaseImpl implements AiMessageCitationPortIn {

    private final CurrentAccountPortIn currentAccountPortIn;
    private final ChatConversationPortOut chatPortOut;
    private final AiMessageCitationPortOut citationPortOut;
    private final KnowledgeRetrievalAuditPortOut auditPortOut;

    public AiMessageCitationUseCaseImpl(
            CurrentAccountPortIn currentAccountPortIn,
            ChatConversationPortOut chatPortOut,
            AiMessageCitationPortOut citationPortOut,
            KnowledgeRetrievalAuditPortOut auditPortOut) {
        this.currentAccountPortIn = currentAccountPortIn;
        this.chatPortOut = chatPortOut;
        this.citationPortOut = citationPortOut;
        this.auditPortOut = auditPortOut;
    }

    @Override
    @Transactional(readOnly = true)
    public MessageCitationsResult citationsForMessage(UUID messageId) {
        UUID accountId = currentAccountPortIn.getCurrentAccountIdOrThrow();
        ChatMessage message = chatPortOut.findMessageById(messageId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy tin nhắn"));
        boolean member = chatPortOut.findMember(message.getConversationId(), accountId).isPresent();
        if (!member) {
            throw new AccessDeniedException("Access is denied");
        }
        List<AiMessageCitation> citations = citationPortOut.findByMessageId(messageId);
        RetrievalAudit audit = citations.stream()
                .map(AiMessageCitation::retrievalAuditId)
                .filter(java.util.Objects::nonNull)
                .findFirst()
                .flatMap(auditPortOut::findById)
                .orElse(null);
        BigDecimal groundedConfidence = audit == null ? null : audit.groundedConfidence();
        String diagnosticCode = audit == null ? null : audit.diagnosticCode();
        boolean handoffRecommended = groundedConfidence != null
                && (groundedConfidence.compareTo(new BigDecimal("0.55")) < 0
                || "INSUFFICIENT_EVIDENCE".equals(diagnosticCode));
        return new MessageCitationsResult(
                messageId,
                citations,
                groundedConfidence,
                handoffRecommended,
                diagnosticCode
        );
    }
}
