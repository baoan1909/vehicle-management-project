package com.ban.vehicle_management.application.ai.service;

import com.ban.vehicle_management.application.ai.port.in.AiMessageCitationPortIn;
import com.ban.vehicle_management.application.ai.port.out.AiMessageCitationPortOut;
import com.ban.vehicle_management.application.iam.account.port.in.CurrentAccountPortIn;
import com.ban.vehicle_management.application.operations.chatconversation.port.out.ChatConversationPortOut;
import com.ban.vehicle_management.domain.ai.model.AiMessageCitation;
import com.ban.vehicle_management.domain.operations.chatmessage.model.ChatMessage;
import com.ban.vehicle_management.shared.exception.NotFoundException;
import java.util.List;
import java.util.UUID;
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

    public AiMessageCitationUseCaseImpl(
            CurrentAccountPortIn currentAccountPortIn,
            ChatConversationPortOut chatPortOut,
            AiMessageCitationPortOut citationPortOut) {
        this.currentAccountPortIn = currentAccountPortIn;
        this.chatPortOut = chatPortOut;
        this.citationPortOut = citationPortOut;
    }

    @Override
    @Transactional(readOnly = true)
    public List<AiMessageCitation> citationsForMessage(UUID messageId) {
        UUID accountId = currentAccountPortIn.getCurrentAccountIdOrThrow();
        ChatMessage message = chatPortOut.findMessageById(messageId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy tin nhắn"));
        boolean member = chatPortOut.findMember(message.getConversationId(), accountId).isPresent();
        if (!member) {
            throw new AccessDeniedException("Access is denied");
        }
        return citationPortOut.findByMessageId(messageId);
    }
}
