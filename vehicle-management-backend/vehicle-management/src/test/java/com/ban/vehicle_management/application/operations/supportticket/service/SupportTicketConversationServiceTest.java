package com.ban.vehicle_management.application.operations.supportticket.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ban.vehicle_management.application.operations.chatconversation.mapper.ChatRealtimeEventMapper;
import com.ban.vehicle_management.application.operations.chatconversation.model.ChatRealtimeEvent;
import com.ban.vehicle_management.application.operations.chatconversation.port.out.ChatConversationPortOut;
import com.ban.vehicle_management.application.operations.chatconversation.port.out.ChatRealtimeEventPublisherPortOut;
import com.ban.vehicle_management.application.operations.supportticket.port.out.SupportTicketConversationLinkPortOut;
import com.ban.vehicle_management.domain.operations.chatconversation.model.ChatConversation;
import com.ban.vehicle_management.domain.operations.chatconversation.model.ChatConversationMember;
import com.ban.vehicle_management.domain.operations.chatmessage.model.ChatMessage;
import com.ban.vehicle_management.domain.operations.supportticket.model.SupportTicket;
import com.ban.vehicle_management.shared.enumeration.operations.ChatConversationStatus;
import com.ban.vehicle_management.shared.enumeration.operations.ChatConversationType;
import com.ban.vehicle_management.shared.enumeration.operations.SupportTicketConversationLinkReason;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SupportTicketConversationServiceTest {

    @Mock ChatConversationPortOut chatPortOut;
    @Mock SupportTicketConversationLinkPortOut linkPortOut;
    @Mock ChatRealtimeEventPublisherPortOut realtimePublisher;
    @Mock ChatRealtimeEventMapper realtimeMapper;
    @InjectMocks SupportTicketConversationService service;

    @Test
    void reusesAndRestoresArchivedAssistantConversation() {
        UUID customerId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();
        ChatConversation archived = conversation(ChatConversationType.ASSISTANT_SUPPORT, customerId, accountId);
        archived.setStatus(ChatConversationStatus.ARCHIVED);
        archived.setTitle("Old title");
        when(chatPortOut.findAssistantSupportConversation(customerId)).thenReturn(Optional.of(archived));
        when(chatPortOut.saveConversation(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(chatPortOut.findMember(archived.getConversationId(), accountId)).thenReturn(Optional.empty());
        when(chatPortOut.saveMember(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(chatPortOut.findConversationById(archived.getConversationId())).thenReturn(Optional.of(archived));

        ChatConversation result = service.openOrCreateAssistantConversation(customerId, accountId);

        assertSame(archived, result);
        assertEquals(ChatConversationStatus.ACTIVE, archived.getStatus());
        assertEquals("Trợ lý hỗ trợ CoParking", archived.getTitle());
        verify(chatPortOut).lockCustomerSupport(customerId);
    }

    @Test
    void openingReplyCreatesLinkButDoesNotDuplicateExistingTicketCard() {
        UUID customerId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();
        SupportTicket ticket = ticket(customerId, employeeId);
        ChatConversation direct = conversation(ChatConversationType.CUSTOMER_DIRECT, customerId, employeeId);
        when(chatPortOut.findActiveCustomerSupportConversation(customerId, employeeId)).thenReturn(Optional.of(direct));
        when(linkPortOut.findMostRecentBySupportTicketId(ticket.getSupportTicketId())).thenReturn(Optional.empty());
        when(linkPortOut.existsBySupportTicketId(ticket.getSupportTicketId())).thenReturn(false);
        when(chatPortOut.existsSupportTicketCard(direct.getConversationId(), ticket.getSupportTicketId())).thenReturn(true);
        when(chatPortOut.findConversationById(direct.getConversationId())).thenReturn(Optional.of(direct));

        ChatConversation result = service.openPrivateConversationForReply(ticket, employeeId);

        assertSame(direct, result);
        verify(linkPortOut).activate(ticket.getSupportTicketId(), direct.getConversationId(),
                SupportTicketConversationLinkReason.FIRST_REPLY, employeeId);
        verify(chatPortOut, never()).saveMessage(any());
    }

    @Test
    void newTicketCardIsPublishedForRealtimeAfterPersistence() {
        UUID customerId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();
        SupportTicket ticket = ticket(customerId, employeeId);
        ChatConversation direct = conversation(ChatConversationType.CUSTOMER_DIRECT, customerId, employeeId);
        when(chatPortOut.findActiveCustomerSupportConversation(customerId, employeeId)).thenReturn(Optional.of(direct));
        when(linkPortOut.findMostRecentBySupportTicketId(ticket.getSupportTicketId())).thenReturn(Optional.empty());
        when(linkPortOut.existsBySupportTicketId(ticket.getSupportTicketId())).thenReturn(false);
        when(chatPortOut.existsSupportTicketCard(direct.getConversationId(), ticket.getSupportTicketId())).thenReturn(false);
        when(chatPortOut.saveMessage(any())).thenAnswer(invocation -> {
            ChatMessage message = invocation.getArgument(0);
            message.setCreatedAt(Instant.now());
            return message;
        });
        when(chatPortOut.saveConversation(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(chatPortOut.findConversationById(direct.getConversationId())).thenReturn(Optional.of(direct));
        ChatRealtimeEvent event = new ChatRealtimeEvent(direct.getConversationId(), UUID.randomUUID(), Instant.now(), null);
        when(realtimeMapper.toRealtimeEvent(any(), any())).thenReturn(event);

        service.openPrivateConversationForReply(ticket, employeeId);

        verify(realtimePublisher).publish(eq(event));
    }

    @Test
    void customerCannotResolveConversationMetadataWithoutActiveMembership() {
        SupportTicket ticket = ticket(UUID.randomUUID(), UUID.randomUUID());
        UUID conversationId = UUID.randomUUID();
        UUID customerAccountId = UUID.randomUUID();
        com.ban.vehicle_management.domain.operations.supportticket.model.SupportTicketConversationLink link =
                new com.ban.vehicle_management.domain.operations.supportticket.model.SupportTicketConversationLink();
        link.setConversationId(conversationId);
        when(linkPortOut.findActiveBySupportTicketId(ticket.getSupportTicketId())).thenReturn(Optional.of(link));
        when(chatPortOut.existsActiveMember(conversationId, customerAccountId)).thenReturn(false);

        assertThrows(org.springframework.security.access.AccessDeniedException.class,
                () -> service.getActivePrivateConversation(ticket, customerAccountId));
        verify(chatPortOut, never()).findConversationById(any());
    }

    private ChatConversation conversation(ChatConversationType type, UUID customerId, UUID ownerId) {
        ChatConversation conversation = new ChatConversation();
        conversation.setConversationId(UUID.randomUUID());
        conversation.setConversationType(type);
        conversation.setCustomerId(customerId);
        conversation.setOwnerAccountId(ownerId);
        conversation.setAssignedTo(type == ChatConversationType.CUSTOMER_DIRECT ? ownerId : null);
        conversation.setStatus(ChatConversationStatus.ACTIVE);
        return conversation;
    }

    private SupportTicket ticket(UUID customerId, UUID employeeId) {
        SupportTicket ticket = new SupportTicket();
        ticket.setSupportTicketId(UUID.randomUUID());
        ticket.setCustomerId(customerId);
        ticket.setAssignedTo(employeeId);
        ticket.setContent("Parking support request");
        return ticket;
    }
}
