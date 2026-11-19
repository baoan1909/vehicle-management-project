package com.ban.vehicle_management.application.operations.supportticket.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ban.vehicle_management.application.operations.chatconversation.mapper.ChatRealtimeEventMapper;
import com.ban.vehicle_management.application.operations.chatconversation.model.ChatRealtimeEvent;
import com.ban.vehicle_management.application.operations.chatconversation.port.out.ChatConversationPortOut;
import com.ban.vehicle_management.application.operations.chatconversation.port.out.ChatRealtimeEventPublisherPortOut;
import com.ban.vehicle_management.application.operations.supportticket.port.out.SupportTicketConversationLinkPortOut;
import com.ban.vehicle_management.application.operations.supportticket.port.out.SupportTicketEventPortOut;
import com.ban.vehicle_management.application.operations.supportticket.mapper.SupportTicketSnapshotJsonMapper;
import com.ban.vehicle_management.domain.operations.chatconversation.model.ChatConversation;
import com.ban.vehicle_management.domain.operations.chatconversation.model.ChatConversationMember;
import com.ban.vehicle_management.domain.operations.chatmessage.model.ChatMessage;
import com.ban.vehicle_management.domain.operations.supportticket.model.SupportTicket;
import com.ban.vehicle_management.domain.operations.supportticket.model.SupportTicketConversationLink;
import com.ban.vehicle_management.domain.operations.supportticket.model.SupportTicketEvent;
import com.ban.vehicle_management.shared.enumeration.operations.ChatConversationStatus;
import com.ban.vehicle_management.shared.enumeration.operations.ChatConversationType;
import com.ban.vehicle_management.shared.enumeration.operations.SupportTicketConversationLinkReason;
import com.ban.vehicle_management.shared.enumeration.operations.SupportTicketEventType;
import com.ban.vehicle_management.shared.enumeration.operations.SupportTicketStatus;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.mockito.ArgumentCaptor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.CsvSource;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@ExtendWith(MockitoExtension.class)
class SupportTicketConversationServiceTest {

    @Mock ChatConversationPortOut chatPortOut;
    @Mock SupportTicketConversationLinkPortOut linkPortOut;
    @Mock SupportTicketEventPortOut ticketEventPortOut;
    @Mock SupportTicketSnapshotJsonMapper snapshotJsonMapper;
    @Mock ChatRealtimeEventPublisherPortOut realtimePublisher;
    @Mock ChatRealtimeEventMapper realtimeMapper;
    @InjectMocks SupportTicketConversationService service;

    @Test
    void reusesAndRestoresArchivedAssistantConversation() {
        UUID accountId = UUID.randomUUID();
        ChatConversation archived = conversation(ChatConversationType.ASSISTANT_SUPPORT, accountId);
        archived.setStatus(ChatConversationStatus.ARCHIVED);
        archived.setTitle("Old title");
        when(chatPortOut.findAssistantSupportConversationByOwnerAccountId(accountId)).thenReturn(Optional.of(archived));
        when(chatPortOut.saveConversation(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(chatPortOut.findMember(archived.getConversationId(), accountId)).thenReturn(Optional.empty());
        when(chatPortOut.saveMember(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(chatPortOut.findConversationById(archived.getConversationId())).thenReturn(Optional.of(archived));

        ChatConversation result = service.openOrCreateAssistantConversation(accountId);

        assertSame(archived, result);
        assertEquals(ChatConversationStatus.ACTIVE, archived.getStatus());
        assertEquals("Trợ lý hỗ trợ CoParking", archived.getTitle());
        verify(chatPortOut).lockRequesterSupport(accountId);
    }

    @Test
    void openingReplyCreatesLinkAndResurfacesTicketCard() {
        UUID requesterAccountId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();
        SupportTicket ticket = ticket(requesterAccountId, employeeId);
        ChatConversation direct = conversation(ChatConversationType.SUPPORT_REQUESTER_DIRECT, requesterAccountId);
        when(chatPortOut.findActiveRequesterSupportConversation(requesterAccountId, employeeId)).thenReturn(Optional.of(direct));
        when(linkPortOut.findMostRecentBySupportTicketId(ticket.getSupportTicketId())).thenReturn(Optional.empty());
        when(linkPortOut.existsBySupportTicketId(ticket.getSupportTicketId())).thenReturn(false);
        when(chatPortOut.saveMessage(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(chatPortOut.saveConversation(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(chatPortOut.findConversationById(direct.getConversationId())).thenReturn(Optional.of(direct));

        ChatConversation result = service.openRequesterConversationForReply(
                ticket, employeeId, SupportTicketStatus.IN_PROGRESS, "open-1"
        );

        assertSame(direct, result);
        verify(linkPortOut).activate(ticket.getSupportTicketId(), direct.getConversationId(),
                SupportTicketConversationLinkReason.FIRST_REPLY, employeeId);
        verify(chatPortOut).saveMessage(any(ChatMessage.class));
    }

    @Test
    void newTicketCardIsPublishedForRealtimeAfterPersistence() {
        UUID requesterAccountId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();
        SupportTicket ticket = ticket(requesterAccountId, employeeId);
        ChatConversation direct = conversation(ChatConversationType.SUPPORT_REQUESTER_DIRECT, requesterAccountId);
        when(chatPortOut.findActiveRequesterSupportConversation(requesterAccountId, employeeId)).thenReturn(Optional.of(direct));
        when(linkPortOut.findMostRecentBySupportTicketId(ticket.getSupportTicketId())).thenReturn(Optional.empty());
        when(linkPortOut.existsBySupportTicketId(ticket.getSupportTicketId())).thenReturn(false);
        when(chatPortOut.saveMessage(any())).thenAnswer(invocation -> {
            ChatMessage message = invocation.getArgument(0);
            message.setCreatedAt(Instant.now());
            return message;
        });
        when(chatPortOut.saveConversation(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(chatPortOut.findConversationById(direct.getConversationId())).thenReturn(Optional.of(direct));
        ChatRealtimeEvent event = new ChatRealtimeEvent(direct.getConversationId(), UUID.randomUUID(), Instant.now(), null);
        when(realtimeMapper.toRealtimeEvent(any(), any())).thenReturn(event);

        TransactionSynchronizationManager.initSynchronization();
        try {
            service.openRequesterConversationForReply(
                    ticket, employeeId, SupportTicketStatus.IN_PROGRESS, "open-2"
            );
            verify(realtimePublisher, never()).publish(any());
            TransactionSynchronizationManager.getSynchronizations()
                    .forEach(TransactionSynchronization::afterCommit);
            verify(realtimePublisher).publish(eq(event));
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void reopeningFromPrivateConversationRefreshesLinkAndRepostsTicketCard() {
        UUID requesterAccountId = UUID.randomUUID();
        UUID reopenedByAccountId = UUID.randomUUID();
        SupportTicket ticket = ticket(requesterAccountId, reopenedByAccountId);
        ChatConversation direct = conversation(ChatConversationType.SUPPORT_REQUESTER_DIRECT, requesterAccountId);
        SupportTicketConversationLink activeLink = new SupportTicketConversationLink();
        activeLink.setSupportTicketId(ticket.getSupportTicketId());
        activeLink.setConversationId(direct.getConversationId());
        when(linkPortOut.findActiveBySupportTicketId(ticket.getSupportTicketId())).thenReturn(Optional.of(activeLink));
        when(chatPortOut.findConversationById(direct.getConversationId())).thenReturn(Optional.of(direct));
        when(chatPortOut.existsActiveMember(direct.getConversationId(), reopenedByAccountId)).thenReturn(true);
        when(chatPortOut.saveMessage(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(chatPortOut.saveConversation(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.repostReopenedTicketCard(
                ticket, reopenedByAccountId, SupportTicketStatus.RESOLVED, "reopen-1"
        );

        verify(linkPortOut).deactivate(ticket.getSupportTicketId());
        verify(linkPortOut).activate(
                ticket.getSupportTicketId(),
                direct.getConversationId(),
                SupportTicketConversationLinkReason.REOPENED,
                reopenedByAccountId
        );
        verify(chatPortOut).saveMessage(any(ChatMessage.class));
    }

    @Test
    void sharingHistoryTicketRefreshesConversationLinkAndPostsNewCard() {
        UUID requesterAccountId = UUID.randomUUID();
        UUID handlerAccountId = UUID.randomUUID();
        SupportTicket ticket = ticket(requesterAccountId, handlerAccountId);
        ChatConversation direct = conversation(ChatConversationType.SUPPORT_REQUESTER_DIRECT, requesterAccountId);
        when(chatPortOut.existsActiveMember(direct.getConversationId(), handlerAccountId)).thenReturn(true);
        when(linkPortOut.findMostRecentBySupportTicketId(ticket.getSupportTicketId())).thenReturn(Optional.empty());
        when(linkPortOut.existsBySupportTicketId(ticket.getSupportTicketId())).thenReturn(false);
        when(chatPortOut.saveMessage(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(chatPortOut.saveConversation(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.repostTicketCardFromConversation(
                ticket,
                direct,
                handlerAccountId,
                SupportTicketEventType.SHARED,
                SupportTicketStatus.IN_PROGRESS,
                "share-1"
        );

        verify(linkPortOut).deactivate(ticket.getSupportTicketId());
        verify(linkPortOut).activate(
                ticket.getSupportTicketId(),
                direct.getConversationId(),
                SupportTicketConversationLinkReason.FIRST_REPLY,
                handlerAccountId
        );
        verify(chatPortOut).saveMessage(any(ChatMessage.class));
    }

    @Test
    void statusChangesCreateSeparateImmutableTicketSnapshotsInTheSameConversation() {
        UUID requesterAccountId = UUID.randomUUID();
        UUID handlerAccountId = UUID.randomUUID();
        SupportTicket ticket = ticket(requesterAccountId, handlerAccountId);
        ChatConversation direct = conversation(ChatConversationType.SUPPORT_REQUESTER_DIRECT, requesterAccountId);
        SupportTicketConversationLink activeLink = new SupportTicketConversationLink();
        activeLink.setSupportTicketId(ticket.getSupportTicketId());
        activeLink.setConversationId(direct.getConversationId());

        when(linkPortOut.findActiveBySupportTicketId(ticket.getSupportTicketId())).thenReturn(Optional.of(activeLink));
        when(chatPortOut.findConversationById(direct.getConversationId())).thenReturn(Optional.of(direct));
        when(chatPortOut.existsActiveMember(direct.getConversationId(), handlerAccountId)).thenReturn(true);
        when(snapshotJsonMapper.toJson(any(), any(), eq(ticket), eq(handlerAccountId), any()))
                .thenAnswer(invocation -> "{\"snapshotStatus\":\"" + ticket.getStatus() + "\"}");
        when(chatPortOut.saveMessage(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(chatPortOut.saveConversation(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.postTicketStatusCard(
                ticket,
                handlerAccountId,
                SupportTicketEventType.STARTED_PROCESSING,
                SupportTicketStatus.OPEN,
                "processing-event"
        );
        ticket.setStatus(SupportTicketStatus.RESOLVED);
        service.postTicketStatusCard(
                ticket,
                handlerAccountId,
                SupportTicketEventType.RESOLVED,
                SupportTicketStatus.IN_PROGRESS,
                "resolved-event"
        );

        ArgumentCaptor<SupportTicketEvent> eventCaptor = ArgumentCaptor.forClass(SupportTicketEvent.class);
        verify(ticketEventPortOut, times(2)).save(eventCaptor.capture());
        SupportTicketEvent processingEvent = eventCaptor.getAllValues().get(0);
        SupportTicketEvent resolvedEvent = eventCaptor.getAllValues().get(1);
        assertNotEquals(processingEvent.getEventId(), resolvedEvent.getEventId());
        assertEquals(SupportTicketStatus.IN_PROGRESS, processingEvent.getSnapshotStatus());
        assertEquals(SupportTicketStatus.RESOLVED, resolvedEvent.getSnapshotStatus());
        assertEquals("processing-event", processingEvent.getIdempotencyKey());
        assertEquals("resolved-event", resolvedEvent.getIdempotencyKey());

        ArgumentCaptor<ChatMessage> messageCaptor = ArgumentCaptor.forClass(ChatMessage.class);
        verify(chatPortOut, times(2)).saveMessage(messageCaptor.capture());
        assertEquals(processingEvent.getEventId(), messageCaptor.getAllValues().get(0).getSupportTicketEventId());
        assertEquals(resolvedEvent.getEventId(), messageCaptor.getAllValues().get(1).getSupportTicketEventId());
        assertEquals("{\"snapshotStatus\":\"IN_PROGRESS\"}", messageCaptor.getAllValues().get(0).getMetadata());
        assertEquals("{\"snapshotStatus\":\"RESOLVED\"}", messageCaptor.getAllValues().get(1).getMetadata());
    }

    @Test
    void retryingTheSameIdempotencyKeyDoesNotCreateAnotherEventOrMessage() {
        UUID requesterAccountId = UUID.randomUUID();
        UUID handlerAccountId = UUID.randomUUID();
        SupportTicket ticket = ticket(requesterAccountId, handlerAccountId);
        ChatConversation direct = conversation(ChatConversationType.SUPPORT_REQUESTER_DIRECT, requesterAccountId);
        SupportTicketConversationLink activeLink = new SupportTicketConversationLink();
        activeLink.setSupportTicketId(ticket.getSupportTicketId());
        activeLink.setConversationId(direct.getConversationId());

        when(linkPortOut.findActiveBySupportTicketId(ticket.getSupportTicketId())).thenReturn(Optional.of(activeLink));
        when(chatPortOut.findConversationById(direct.getConversationId())).thenReturn(Optional.of(direct));
        when(chatPortOut.existsActiveMember(direct.getConversationId(), handlerAccountId)).thenReturn(true);
        when(snapshotJsonMapper.toJson(any(), any(), eq(ticket), eq(handlerAccountId), any()))
                .thenReturn("{\"snapshotStatus\":\"IN_PROGRESS\"}");
        when(chatPortOut.saveMessage(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(chatPortOut.saveConversation(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.postTicketStatusCard(
                ticket,
                handlerAccountId,
                SupportTicketEventType.STARTED_PROCESSING,
                SupportTicketStatus.OPEN,
                "same-key"
        );

        ArgumentCaptor<SupportTicketEvent> eventCaptor = ArgumentCaptor.forClass(SupportTicketEvent.class);
        verify(ticketEventPortOut).save(eventCaptor.capture());
        SupportTicketEvent existingEvent = eventCaptor.getValue();
        ChatMessage existingMessage = new ChatMessage();
        existingMessage.setMessageId(UUID.randomUUID());
        existingMessage.setSupportTicketEventId(existingEvent.getEventId());
        when(ticketEventPortOut.findByConversationIdAndIdempotencyKey(direct.getConversationId(), "same-key"))
                .thenReturn(Optional.of(existingEvent));
        when(chatPortOut.findMessageBySupportTicketEventId(existingEvent.getEventId()))
                .thenReturn(Optional.of(existingMessage));

        service.postTicketStatusCard(
                ticket,
                handlerAccountId,
                SupportTicketEventType.STARTED_PROCESSING,
                SupportTicketStatus.OPEN,
                "same-key"
        );

        verify(ticketEventPortOut, times(1)).save(any(SupportTicketEvent.class));
        verify(chatPortOut, times(1)).saveMessage(any(ChatMessage.class));
        verify(snapshotJsonMapper, times(1)).toJson(any(), any(), eq(ticket), eq(handlerAccountId), any());
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
                () -> service.getActiveRequesterConversation(ticket, customerAccountId));
        verify(chatPortOut, never()).findConversationById(any());
    }

    @ParameterizedTest
    @EnumSource(value = ChatConversationType.class, names = {"ASSISTANT_SUPPORT", "SUPPORT_REQUESTER_DIRECT"}, mode = EnumSource.Mode.EXCLUDE)
    void ordinaryChatsCannotCreateOrReceiveSupportTicketCards(ChatConversationType type) {
        UUID accountId = UUID.randomUUID();
        ChatConversation ordinary = conversation(type, accountId);
        SupportTicket ticket = ticket(accountId, UUID.randomUUID());
        when(chatPortOut.findConversationById(ordinary.getConversationId())).thenReturn(Optional.of(ordinary));

        assertThrows(BadRequestException.class,
                () -> service.getRequesterTicketOriginConversation(ordinary.getConversationId(), accountId));
        assertThrows(BadRequestException.class,
                () -> service.postTicketCardFromRequester(ticket, ordinary, accountId));
        assertThrows(BadRequestException.class,
                () -> service.repostTicketCardFromRequester(ticket, ordinary, accountId, "retry"));
        assertThrows(BadRequestException.class,
                () -> service.repostTicketCardFromConversation(ticket, ordinary, accountId,
                        SupportTicketEventType.SHARED, ticket.getStatus(), "share"));

        verify(chatPortOut, never()).saveMessage(any());
        org.mockito.Mockito.verifyNoInteractions(linkPortOut, ticketEventPortOut, snapshotJsonMapper, realtimePublisher);
    }

    @ParameterizedTest
    @CsvSource({"ASSISTANT_SUPPORT,ARCHIVED", "ASSISTANT_SUPPORT,CLOSED",
            "SUPPORT_REQUESTER_DIRECT,ARCHIVED", "SUPPORT_REQUESTER_DIRECT,CLOSED"})
    void inactiveSupportConversationCannotReceiveNewTicketCards(ChatConversationType type, ChatConversationStatus status) {
        UUID accountId = UUID.randomUUID();
        ChatConversation inactive = conversation(type, accountId);
        inactive.setStatus(status);
        SupportTicket ticket = ticket(accountId, UUID.randomUUID());
        when(chatPortOut.findConversationById(inactive.getConversationId())).thenReturn(Optional.of(inactive));

        assertThrows(BadRequestException.class,
                () -> service.getRequesterTicketOriginConversation(inactive.getConversationId(), accountId));
        assertThrows(BadRequestException.class,
                () -> service.postTicketCardFromRequester(ticket, inactive, accountId));
        assertThrows(BadRequestException.class,
                () -> service.repostTicketCardFromRequester(ticket, inactive, accountId, "retry"));
        assertThrows(BadRequestException.class,
                () -> service.repostTicketCardFromConversation(ticket, inactive, accountId,
                        SupportTicketEventType.SHARED, ticket.getStatus(), "share"));
        verify(chatPortOut, never()).saveMessage(any());
    }

    @ParameterizedTest
    @EnumSource(value = ChatConversationType.class, names = {"ASSISTANT_SUPPORT", "SUPPORT_REQUESTER_DIRECT"})
    void activeSystemSupportConversationRemainsAValidTicketOrigin(ChatConversationType type) {
        UUID accountId = UUID.randomUUID();
        ChatConversation support = conversation(type, accountId);
        when(chatPortOut.findConversationById(support.getConversationId())).thenReturn(Optional.of(support));
        when(chatPortOut.existsActiveMember(support.getConversationId(), accountId)).thenReturn(true);
        assertSame(support, service.getRequesterTicketOriginConversation(support.getConversationId(), accountId));
    }

    private ChatConversation conversation(ChatConversationType type, UUID ownerId) {
        ChatConversation conversation = new ChatConversation();
        conversation.setConversationId(UUID.randomUUID());
        conversation.setConversationType(type);
        conversation.setOwnerAccountId(ownerId);
        conversation.setStatus(ChatConversationStatus.ACTIVE);
        return conversation;
    }

    private SupportTicket ticket(UUID requesterAccountId, UUID employeeId) {
        SupportTicket ticket = new SupportTicket();
        ticket.setSupportTicketId(UUID.randomUUID());
        ticket.setRequesterAccountId(requesterAccountId);
        ticket.setRequesterRoleCode("CUSTOMER");
        ticket.setAssignedTo(employeeId);
        ticket.setStatus(SupportTicketStatus.IN_PROGRESS);
        ticket.setContent("Parking support request");
        return ticket;
    }
}
