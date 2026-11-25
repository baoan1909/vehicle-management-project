package com.ban.vehicle_management.application.operations.supportticket.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verifyNoInteractions;

import com.ban.vehicle_management.application.iam.account.port.in.CurrentAccountPortIn;
import com.ban.vehicle_management.application.operations.supportticket.port.out.SupportTicketConversationLinkPortOut;
import com.ban.vehicle_management.application.operations.supportticket.port.out.SupportTicketPortOut;
import com.ban.vehicle_management.domain.operations.chatconversation.model.ChatConversation;
import com.ban.vehicle_management.domain.operations.supportticket.model.SupportTicket;
import com.ban.vehicle_management.domain.operations.supportticket.model.SupportTicketConversationLink;
import com.ban.vehicle_management.shared.enumeration.operations.ChatConversationType;
import com.ban.vehicle_management.shared.enumeration.operations.SupportTicketConversationLinkStatus;
import com.ban.vehicle_management.shared.enumeration.operations.SupportTicketStatus;
import java.util.Optional;
import java.util.UUID;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SupportTicketChatMessageContextServiceTest {

    @Mock
    private CurrentAccountPortIn currentAccountPortIn;
    @Mock
    private SupportTicketPortOut supportTicketPortOut;
    @Mock
    private SupportTicketConversationLinkPortOut linkPortOut;
    @Mock
    private SupportTicketConversationService ticketConversationService;
    @InjectMocks
    private SupportTicketChatMessageContextService service;

    @ParameterizedTest
    @EnumSource(value = ChatConversationType.class, names = {"ASSISTANT_SUPPORT", "SUPPORT_REQUESTER_DIRECT"}, mode = EnumSource.Mode.EXCLUDE)
    void ordinaryChatCannotAttachPrivateSupportTicketContext(ChatConversationType type) {
        ChatConversation conversation = new ChatConversation();
        conversation.setConversationType(type);
        assertThrows(BadRequestException.class,
                () -> service.resolveContextForSend(conversation, UUID.randomUUID(), UUID.randomUUID()));
        verifyNoInteractions(currentAccountPortIn, supportTicketPortOut, linkPortOut, ticketConversationService);
    }

    @ParameterizedTest
    @EnumSource(value = ChatConversationType.class, names = {"ASSISTANT_SUPPORT", "SUPPORT_REQUESTER_DIRECT"}, mode = EnumSource.Mode.EXCLUDE)
    void ordinaryChatWithoutTicketContextIsUnchanged(ChatConversationType type) {
        ChatConversation conversation = new ChatConversation();
        conversation.setConversationType(type);
        assertNull(service.resolveContextForSend(conversation, null, UUID.randomUUID()));
        verifyNoInteractions(currentAccountPortIn, supportTicketPortOut, linkPortOut, ticketConversationService);
    }

    @Test
    void allowsGenericRequesterDirectMessageWithoutTicketContext() {
        ChatConversation conversation = requesterConversation(UUID.randomUUID());

        UUID resolvedContext = service.resolveContextForSend(conversation, null, UUID.randomUUID());

        assertNull(resolvedContext);
        verifyNoInteractions(currentAccountPortIn, supportTicketPortOut, linkPortOut, ticketConversationService);
    }

    @Test
    void firstAssignedSystemAdminMessageStartsTicketAfterReplyLinkWasOpened() {
        UUID requesterAccountId = UUID.randomUUID();
        UUID systemAdminAccountId = UUID.randomUUID();
        UUID ticketId = UUID.randomUUID();
        ChatConversation conversation = requesterConversation(requesterAccountId);
        SupportTicket ticket = openTicket(ticketId, requesterAccountId, systemAdminAccountId);

        when(supportTicketPortOut.findByIdForUpdate(ticketId)).thenReturn(Optional.of(ticket));
        when(supportTicketPortOut.save(any(SupportTicket.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(linkPortOut.findActiveBySupportTicketId(ticketId)).thenReturn(Optional.of(activeLink(ticketId, conversation.getConversationId())));
        when(currentAccountPortIn.hasPermission(anyString())).thenReturn(true);

        UUID resolvedContext = service.resolveContextForSend(conversation, ticketId, systemAdminAccountId);

        assertEquals(ticketId, resolvedContext);
        verify(currentAccountPortIn).hasPermission("SUPPORT_TICKET_READ_ALL");
        verify(currentAccountPortIn).hasPermission("SUPPORT_TICKET_PROCESS_ALL");
        verify(currentAccountPortIn).hasPermission("SUPPORT_TICKET_RESPOND_ASSIGNED");
        assertEquals(SupportTicketStatus.IN_PROGRESS, ticket.getStatus());
        assertNotNull(ticket.getFirstRespondedAt());
        verify(ticketConversationService).postAssistantTicketUpdate(eq(ticket), any(String.class));
    }

    @Test
    void unlinkedTicketContextFallsBackToGenericPrivateChat() {
        UUID requesterAccountId = UUID.randomUUID();
        UUID systemAdminAccountId = UUID.randomUUID();
        UUID ticketId = UUID.randomUUID();
        ChatConversation conversation = requesterConversation(requesterAccountId);
        SupportTicket ticket = openTicket(ticketId, requesterAccountId, systemAdminAccountId);
        when(supportTicketPortOut.findByIdForUpdate(ticketId)).thenReturn(Optional.of(ticket));
        when(linkPortOut.findActiveBySupportTicketId(ticketId)).thenReturn(Optional.empty());

        UUID resolvedContext = service.resolveContextForSend(conversation, ticketId, systemAdminAccountId);

        assertNull(resolvedContext);
        verify(supportTicketPortOut, never()).save(any());
    }

    @Test
    void requesterMessageKeepsLinkedOpenTicketOpen() {
        UUID requesterAccountId = UUID.randomUUID();
        UUID systemAdminAccountId = UUID.randomUUID();
        UUID ticketId = UUID.randomUUID();
        ChatConversation conversation = requesterConversation(requesterAccountId);
        SupportTicket ticket = openTicket(ticketId, requesterAccountId, systemAdminAccountId);
        when(supportTicketPortOut.findByIdForUpdate(ticketId)).thenReturn(Optional.of(ticket));
        when(linkPortOut.findActiveBySupportTicketId(ticketId)).thenReturn(Optional.of(activeLink(ticketId, conversation.getConversationId())));
        when(supportTicketPortOut.save(any(SupportTicket.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UUID resolvedContext = service.resolveContextForSend(conversation, ticketId, requesterAccountId);

        assertEquals(ticketId, resolvedContext);
        assertEquals(SupportTicketStatus.OPEN, ticket.getStatus());
        assertNotNull(ticket.getLastContextActivityAt());
        verify(supportTicketPortOut).save(ticket);
    }

    @Test
    void resolvedTicketContextIsKeptAndExtendsItsOwnDeadline() {
        UUID requesterAccountId = UUID.randomUUID();
        UUID ticketId = UUID.randomUUID();
        ChatConversation conversation = requesterConversation(requesterAccountId);
        SupportTicket ticket = openTicket(ticketId, requesterAccountId, UUID.randomUUID());
        Instant resolvedAt = Instant.now().minusSeconds(3600);
        ticket.setStatus(SupportTicketStatus.RESOLVED);
        ticket.setResolvedAt(resolvedAt);
        ticket.setAutoCloseAt(resolvedAt.plusSeconds(86400));
        when(supportTicketPortOut.findByIdForUpdate(ticketId)).thenReturn(Optional.of(ticket));
        when(linkPortOut.findActiveBySupportTicketId(ticketId)).thenReturn(Optional.of(activeLink(ticketId, conversation.getConversationId())));
        when(supportTicketPortOut.save(any(SupportTicket.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UUID resolvedContext = service.resolveContextForSend(conversation, ticketId, requesterAccountId);

        assertEquals(ticketId, resolvedContext);
        assertNotNull(ticket.getLastContextActivityAt());
        assertEquals(ticket.getLastContextActivityAt().plusSeconds(86400), ticket.getAutoCloseAt());
        verify(supportTicketPortOut).save(ticket);
        verifyNoInteractions(currentAccountPortIn, ticketConversationService);
    }

    @Test
    void contextualMessageForTicketADoesNotExtendTicketBInSameConversation() {
        UUID requesterAccountId = UUID.randomUUID();
        ChatConversation conversation = requesterConversation(requesterAccountId);
        SupportTicket ticketA = resolvedTicket(UUID.randomUUID(), requesterAccountId);
        SupportTicket ticketB = resolvedTicket(UUID.randomUUID(), requesterAccountId);
        Instant ticketBDeadline = ticketB.getAutoCloseAt();
        when(supportTicketPortOut.findByIdForUpdate(ticketA.getSupportTicketId())).thenReturn(Optional.of(ticketA));
        when(linkPortOut.findActiveBySupportTicketId(ticketA.getSupportTicketId())).thenReturn(Optional.of(
                activeLink(ticketA.getSupportTicketId(), conversation.getConversationId())
        ));
        when(supportTicketPortOut.save(ticketA)).thenReturn(ticketA);

        assertEquals(
                ticketA.getSupportTicketId(),
                service.resolveContextForSend(conversation, ticketA.getSupportTicketId(), requesterAccountId)
        );

        assertNotNull(ticketA.getLastContextActivityAt());
        assertNull(ticketB.getLastContextActivityAt());
        assertEquals(ticketBDeadline, ticketB.getAutoCloseAt());
        verify(supportTicketPortOut, never()).save(ticketB);
    }

    @Test
    void closedTicketContextFallsBackToGenericPrivateChat() {
        UUID requesterAccountId = UUID.randomUUID();
        UUID ticketId = UUID.randomUUID();
        ChatConversation conversation = requesterConversation(requesterAccountId);
        SupportTicket ticket = openTicket(ticketId, requesterAccountId, UUID.randomUUID());
        ticket.setStatus(SupportTicketStatus.CLOSED);
        when(supportTicketPortOut.findByIdForUpdate(ticketId)).thenReturn(Optional.of(ticket));

        UUID resolvedContext = service.resolveContextForSend(conversation, ticketId, requesterAccountId);

        assertNull(resolvedContext);
        verifyNoInteractions(currentAccountPortIn, linkPortOut, ticketConversationService);
        verify(supportTicketPortOut, never()).save(any());
    }

    private SupportTicketConversationLink activeLink(UUID ticketId, UUID conversationId) {
        SupportTicketConversationLink link = new SupportTicketConversationLink();
        link.setSupportTicketId(ticketId);
        link.setConversationId(conversationId);
        link.setStatus(SupportTicketConversationLinkStatus.ACTIVE);
        return link;
    }

    private ChatConversation requesterConversation(UUID requesterAccountId) {
        ChatConversation conversation = new ChatConversation();
        conversation.setConversationId(UUID.randomUUID());
        conversation.setOwnerAccountId(requesterAccountId);
        conversation.setConversationType(ChatConversationType.SUPPORT_REQUESTER_DIRECT);
        return conversation;
    }

    private SupportTicket openTicket(UUID ticketId, UUID requesterAccountId, UUID assignedTo) {
        SupportTicket ticket = new SupportTicket();
        ticket.setSupportTicketId(ticketId);
        ticket.setRequesterAccountId(requesterAccountId);
        ticket.setRequesterRoleCode("CUSTOMER");
        ticket.setCategoryId(UUID.randomUUID());
        ticket.setTitle("Need parking support");
        ticket.setContent("The customer needs assistance with a parking issue.");
        ticket.setAssignedTo(assignedTo);
        ticket.setStatus(SupportTicketStatus.OPEN);
        ticket.setReopenCount(0);
        return ticket;
    }

    private SupportTicket resolvedTicket(UUID ticketId, UUID requesterAccountId) {
        SupportTicket ticket = openTicket(ticketId, requesterAccountId, UUID.randomUUID());
        Instant resolvedAt = Instant.parse("2026-11-17T00:00:00Z");
        ticket.setStatus(SupportTicketStatus.RESOLVED);
        ticket.setResolvedAt(resolvedAt);
        ticket.setAutoCloseAt(resolvedAt.plusSeconds(86_400));
        return ticket;
    }
}
