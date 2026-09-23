package com.ban.vehicle_management.application.ai.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import com.ban.vehicle_management.application.ai.port.out.AiMessageCitationPortOut;
import com.ban.vehicle_management.application.iam.account.port.in.CurrentAccountPortIn;
import com.ban.vehicle_management.application.operations.chatconversation.port.out.ChatConversationPortOut;
import com.ban.vehicle_management.domain.ai.model.AiMessageCitation;
import com.ban.vehicle_management.domain.operations.chatconversation.model.ChatConversationMember;
import com.ban.vehicle_management.domain.operations.chatmessage.model.ChatMessage;
import com.ban.vehicle_management.shared.exception.NotFoundException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

@ExtendWith(MockitoExtension.class)
class AiMessageCitationUseCaseImplTest {

    @Mock
    private CurrentAccountPortIn currentAccountPortIn;
    @Mock
    private ChatConversationPortOut chatPortOut;
    @Mock
    private AiMessageCitationPortOut citationPortOut;

    private AiMessageCitationUseCaseImpl useCase;
    private final UUID accountId = UUID.randomUUID();
    private final UUID conversationId = UUID.randomUUID();
    private final UUID messageId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        useCase = new AiMessageCitationUseCaseImpl(currentAccountPortIn, chatPortOut, citationPortOut);
    }

    @Test
    void citationsForMessageShouldReturnCitationsForMembers() {
        ChatMessage message = new ChatMessage();
        message.setMessageId(messageId);
        message.setConversationId(conversationId);
        AiMessageCitation citation = new AiMessageCitation(
                UUID.randomUUID(), messageId, UUID.randomUUID(), UUID.randomUUID(),
                "C1", "Quy trình", 1, "Mục 1", null, UUID.randomUUID(), UUID.randomUUID(), 0);
        when(currentAccountPortIn.getCurrentAccountIdOrThrow()).thenReturn(accountId);
        when(chatPortOut.findMessageById(messageId)).thenReturn(Optional.of(message));
        when(chatPortOut.findMember(conversationId, accountId)).thenReturn(Optional.of(new ChatConversationMember()));
        when(citationPortOut.findByMessageId(messageId)).thenReturn(List.of(citation));

        List<AiMessageCitation> result = useCase.citationsForMessage(messageId);

        assertEquals(1, result.size());
        assertEquals("C1", result.get(0).label());
    }

    @Test
    void citationsForMessageShouldDenyNonMembers() {
        ChatMessage message = new ChatMessage();
        message.setMessageId(messageId);
        message.setConversationId(conversationId);
        when(currentAccountPortIn.getCurrentAccountIdOrThrow()).thenReturn(accountId);
        when(chatPortOut.findMessageById(messageId)).thenReturn(Optional.of(message));
        when(chatPortOut.findMember(conversationId, accountId)).thenReturn(Optional.empty());

        assertThrows(AccessDeniedException.class, () -> useCase.citationsForMessage(messageId));
    }

    @Test
    void citationsForMessageShouldThrowNotFoundForMissingMessage() {
        when(currentAccountPortIn.getCurrentAccountIdOrThrow()).thenReturn(accountId);
        when(chatPortOut.findMessageById(messageId)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> useCase.citationsForMessage(messageId));
    }
}
