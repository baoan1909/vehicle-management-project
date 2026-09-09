package com.ban.vehicle_management.application.operations.supportticket.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ban.vehicle_management.application.notification.notification.port.in.NotificationPortIn;
import com.ban.vehicle_management.application.operations.supportticket.authorization.SupportTicketAccessGuard;
import com.ban.vehicle_management.application.operations.supportticket.model.SupportTicketChatIntake;
import com.ban.vehicle_management.application.operations.supportticket.port.out.SupportTicketConversationLinkPortOut;
import com.ban.vehicle_management.application.operations.supportticket.port.out.SupportTicketPortOut;
import com.ban.vehicle_management.application.operations.supportticket.usecase.SupportTicketUseCaseImpl;
import com.ban.vehicle_management.application.people.customer.port.out.CustomerPortOut;
import com.ban.vehicle_management.domain.operations.chatconversation.model.ChatConversation;
import com.ban.vehicle_management.domain.operations.supportticket.model.SupportTicket;
import com.ban.vehicle_management.shared.enumeration.operations.ChatConversationType;
import com.ban.vehicle_management.shared.enumeration.operations.SupportTicketSource;
import com.ban.vehicle_management.shared.enumeration.operations.SupportTicketStatus;
import com.ban.vehicle_management.shared.exception.ConflictException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

@ExtendWith(MockitoExtension.class)
class SupportTicketUseCaseImplTest {

    @Mock SupportTicketPortOut supportTicketPortOut;
    @Mock SupportTicketAccessGuard accessGuard;
    @Mock CustomerPortOut customerPortOut;
    @Mock NotificationPortIn notificationPortIn;
    @Mock SupportTicketConversationService ticketConversationService;
    @Mock SupportTicketConversationLinkPortOut ticketConversationLinkPortOut;
    @InjectMocks SupportTicketUseCaseImpl useCase;

    @Test
    void chatIntakeRetryReusesTicketAndRepairsMissingCardIdempotently() {
        UUID customerId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();
        ChatConversation assistant = assistantConversation(customerId);
        SupportTicket existing = ticket(customerId);
        when(accessGuard.resolveCustomerIdForCreate()).thenReturn(customerId);
        when(accessGuard.currentAccountId()).thenReturn(accountId);
        when(ticketConversationService.openOrCreateAssistantConversation(customerId, accountId)).thenReturn(assistant);
        when(supportTicketPortOut.findByCustomerIdAndIdempotencyKey(customerId, "request-1"))
                .thenReturn(Optional.of(existing));

        SupportTicketChatIntake result = useCase.createChatIntake(new SupportTicket(), " request-1 ");

        assertSame(existing, result.ticket());
        assertSame(assistant, result.conversation());
        assertTrue(result.reusedActiveTicket());
        verify(supportTicketPortOut).lockCustomerSupport(customerId);
        verify(ticketConversationService).postTicketCardFromCustomer(existing, assistant, accountId);
        verify(supportTicketPortOut, never()).save(any());
    }

    @Test
    void newChatIntakePersistsSourceReferencesAndIdempotencyKey() {
        UUID customerId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();
        ChatConversation assistant = assistantConversation(customerId);
        SupportTicket request = ticket(null);
        when(accessGuard.resolveCustomerIdForCreate()).thenReturn(customerId);
        when(accessGuard.currentAccountId()).thenReturn(accountId);
        when(ticketConversationService.openOrCreateAssistantConversation(customerId, accountId)).thenReturn(assistant);
        when(supportTicketPortOut.findByCustomerIdAndIdempotencyKey(customerId, "request-2"))
                .thenReturn(Optional.empty());
        when(supportTicketPortOut.existsActiveCategoryById(request.getCategoryId())).thenReturn(true);
        when(supportTicketPortOut.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(customerPortOut.findAccountIdByCustomerId(customerId)).thenReturn(Optional.of(accountId));

        SupportTicketChatIntake result = useCase.createChatIntake(request, "request-2");

        assertFalse(result.reusedActiveTicket());
        assertEquals(SupportTicketSource.ASSISTANT_CHAT, result.ticket().getSource());
        assertEquals(assistant.getConversationId(), result.ticket().getSourceConversationId());
        assertEquals("request-2", result.ticket().getIdempotencyKey());
        assertEquals(SupportTicketStatus.OPEN, result.ticket().getStatus());
        verify(ticketConversationService).postTicketCardFromCustomer(result.ticket(), assistant, accountId);
    }

    @Test
    void myTicketsAlwaysUsesCustomerDerivedFromAuthenticatedAccount() {
        UUID customerId = UUID.randomUUID();
        SupportTicket owned = ticket(customerId);
        when(accessGuard.resolveCustomerIdForOwnTickets()).thenReturn(customerId);
        when(supportTicketPortOut.findAll(customerId, null, null, SupportTicketStatus.OPEN, null, "gate"))
                .thenReturn(List.of(owned));

        List<SupportTicket> result = useCase.getMyTickets(SupportTicketStatus.OPEN, " gate ");

        assertEquals(List.of(owned), result);
    }

    @Test
    void customerCannotShareAnotherCustomersTicketIntoAssistantConversation() {
        UUID currentCustomerId = UUID.randomUUID();
        SupportTicket foreignTicket = ticket(UUID.randomUUID());
        when(accessGuard.resolveCustomerIdForAssistant()).thenReturn(currentCustomerId);
        when(supportTicketPortOut.findById(foreignTicket.getSupportTicketId())).thenReturn(Optional.of(foreignTicket));

        assertThrows(AccessDeniedException.class,
                () -> useCase.shareTicketWithAssistant(foreignTicket.getSupportTicketId()));

        verify(ticketConversationService, never()).openOrCreateAssistantConversation(any(), any());
        verify(ticketConversationService, never()).postTicketCardFromCustomer(any(), any(), any());
    }

    @Test
    void sharingOwnedTicketOnlyAddsReferenceCardAndPreservesLifecycle() {
        UUID customerId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();
        UUID assignee = UUID.randomUUID();
        SupportTicket ownedTicket = ticket(customerId);
        ownedTicket.setAssignedTo(assignee);
        ownedTicket.setStatus(SupportTicketStatus.IN_PROGRESS);
        ChatConversation assistant = assistantConversation(customerId);
        when(accessGuard.resolveCustomerIdForAssistant()).thenReturn(customerId);
        when(accessGuard.currentAccountId()).thenReturn(accountId);
        when(supportTicketPortOut.findById(ownedTicket.getSupportTicketId())).thenReturn(Optional.of(ownedTicket));
        when(ticketConversationService.openOrCreateAssistantConversation(customerId, accountId)).thenReturn(assistant);

        SupportTicket result = useCase.shareTicketWithAssistant(ownedTicket.getSupportTicketId());

        assertSame(ownedTicket, result);
        assertEquals(assignee, result.getAssignedTo());
        assertEquals(SupportTicketStatus.IN_PROGRESS, result.getStatus());
        verify(ticketConversationService).postTicketCardFromCustomer(ownedTicket, assistant, accountId);
        verify(supportTicketPortOut, never()).save(any());
    }

    @Test
    void manualStartProgressIsRejectedBecauseFirstStaffReplyOwnsTheTransition() {
        assertThrows(ConflictException.class, () -> useCase.startProgress(UUID.randomUUID()));

        verify(supportTicketPortOut, never()).save(any());
    }

    private ChatConversation assistantConversation(UUID customerId) {
        ChatConversation conversation = new ChatConversation();
        conversation.setConversationId(UUID.randomUUID());
        conversation.setCustomerId(customerId);
        conversation.setConversationType(ChatConversationType.ASSISTANT_SUPPORT);
        return conversation;
    }

    private SupportTicket ticket(UUID customerId) {
        SupportTicket ticket = new SupportTicket();
        ticket.setSupportTicketId(UUID.randomUUID());
        ticket.setCustomerId(customerId);
        ticket.setCategoryId(UUID.randomUUID());
        ticket.setTitle("Barrier gate support");
        ticket.setContent("The barrier gate cannot be opened");
        ticket.setStatus(SupportTicketStatus.OPEN);
        ticket.setReopenCount(0);
        return ticket;
    }
}
