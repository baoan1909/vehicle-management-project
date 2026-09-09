package com.ban.vehicle_management.application.operations.supportticket.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.never;

import com.ban.vehicle_management.application.iam.account.port.in.CurrentAccountPortIn;
import com.ban.vehicle_management.application.operations.chatconversation.port.out.ChatConversationPortOut;
import com.ban.vehicle_management.application.operations.supportticket.port.out.SupportTicketConversationLinkPortOut;
import com.ban.vehicle_management.application.operations.supportticket.port.out.SupportTicketPortOut;
import com.ban.vehicle_management.domain.operations.chatconversation.model.ChatConversation;
import com.ban.vehicle_management.domain.operations.supportticket.model.SupportTicket;
import com.ban.vehicle_management.domain.operations.supportticket.model.SupportTicketConversationLink;
import com.ban.vehicle_management.shared.enumeration.operations.ChatConversationType;
import com.ban.vehicle_management.shared.enumeration.operations.SupportTicketConversationLinkStatus;
import com.ban.vehicle_management.shared.enumeration.operations.SupportTicketStatus;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SupportTicketChatMessageContextServiceTest {

    @Mock
    private CurrentAccountPortIn currentAccountPortIn;
    @Mock
    private ChatConversationPortOut chatPortOut;
    @Mock
    private SupportTicketPortOut supportTicketPortOut;
    @Mock
    private SupportTicketConversationLinkPortOut linkPortOut;
    @Mock
    private SupportTicketConversationService ticketConversationService;
    @InjectMocks
    private SupportTicketChatMessageContextService service;

    @Test
    void rejectsCustomerDirectMessageWithoutTicketContext() {
        ChatConversation conversation = customerConversation(UUID.randomUUID());

        assertThrows(BadRequestException.class, () -> service.ensureCanSend(conversation, null, UUID.randomUUID()));
    }

    @Test
    void firstAssignedEmployeeMessageStartsTicketAfterReplyLinkWasOpened() {
        UUID customerId = UUID.randomUUID();
        UUID customerAccountId = UUID.randomUUID();
        UUID employeeAccountId = UUID.randomUUID();
        UUID ticketId = UUID.randomUUID();
        ChatConversation conversation = customerConversation(customerId);
        SupportTicket ticket = openTicket(ticketId, customerId, employeeAccountId);

        when(supportTicketPortOut.findById(ticketId)).thenReturn(Optional.of(ticket));
        when(chatPortOut.findAccountIdByCustomerId(customerId)).thenReturn(Optional.of(customerAccountId));
        when(supportTicketPortOut.save(any(SupportTicket.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(linkPortOut.findActiveBySupportTicketId(ticketId)).thenReturn(Optional.of(activeLink(ticketId, conversation.getConversationId())));

        service.ensureCanSend(conversation, ticketId, employeeAccountId);

        verify(currentAccountPortIn).requirePermission("SUPPORT_TICKET_READ_ASSIGNED");
        verify(currentAccountPortIn).requirePermission("SUPPORT_TICKET_PROCESS_ASSIGNED");
        verify(currentAccountPortIn).requirePermission("SUPPORT_TICKET_RESPOND_ASSIGNED");
        assertEquals(SupportTicketStatus.IN_PROGRESS, ticket.getStatus());
        assertNotNull(ticket.getFirstRespondedAt());
        verify(ticketConversationService).postAssistantTicketUpdate(eq(ticket), any(String.class));
    }

    @Test
    void rejectsMessageWhenTicketIsNotActivelyLinkedToConversation() {
        UUID customerId = UUID.randomUUID();
        UUID employeeAccountId = UUID.randomUUID();
        UUID ticketId = UUID.randomUUID();
        ChatConversation conversation = customerConversation(customerId);
        SupportTicket ticket = openTicket(ticketId, customerId, employeeAccountId);
        when(supportTicketPortOut.findById(ticketId)).thenReturn(Optional.of(ticket));
        when(chatPortOut.findAccountIdByCustomerId(customerId)).thenReturn(Optional.of(UUID.randomUUID()));
        when(linkPortOut.findActiveBySupportTicketId(ticketId)).thenReturn(Optional.empty());

        assertThrows(org.springframework.security.access.AccessDeniedException.class,
                () -> service.ensureCanSend(conversation, ticketId, employeeAccountId));
    }

    @Test
    void customerMessageKeepsLinkedOpenTicketOpen() {
        UUID customerId = UUID.randomUUID();
        UUID customerAccountId = UUID.randomUUID();
        UUID employeeAccountId = UUID.randomUUID();
        UUID ticketId = UUID.randomUUID();
        ChatConversation conversation = customerConversation(customerId);
        SupportTicket ticket = openTicket(ticketId, customerId, employeeAccountId);
        when(supportTicketPortOut.findById(ticketId)).thenReturn(Optional.of(ticket));
        when(chatPortOut.findAccountIdByCustomerId(customerId)).thenReturn(Optional.of(customerAccountId));
        when(linkPortOut.findActiveBySupportTicketId(ticketId)).thenReturn(Optional.of(activeLink(ticketId, conversation.getConversationId())));

        service.ensureCanSend(conversation, ticketId, customerAccountId);

        assertEquals(SupportTicketStatus.OPEN, ticket.getStatus());
        verify(supportTicketPortOut, never()).save(any());
    }

    private SupportTicketConversationLink activeLink(UUID ticketId, UUID conversationId) {
        SupportTicketConversationLink link = new SupportTicketConversationLink();
        link.setSupportTicketId(ticketId);
        link.setConversationId(conversationId);
        link.setStatus(SupportTicketConversationLinkStatus.ACTIVE);
        return link;
    }

    private ChatConversation customerConversation(UUID customerId) {
        ChatConversation conversation = new ChatConversation();
        conversation.setConversationId(UUID.randomUUID());
        conversation.setCustomerId(customerId);
        conversation.setConversationType(ChatConversationType.CUSTOMER_DIRECT);
        return conversation;
    }

    private SupportTicket openTicket(UUID ticketId, UUID customerId, UUID assignedTo) {
        SupportTicket ticket = new SupportTicket();
        ticket.setSupportTicketId(ticketId);
        ticket.setCustomerId(customerId);
        ticket.setCategoryId(UUID.randomUUID());
        ticket.setTitle("Need parking support");
        ticket.setContent("The customer needs assistance with a parking issue.");
        ticket.setAssignedTo(assignedTo);
        ticket.setStatus(SupportTicketStatus.OPEN);
        ticket.setReopenCount(0);
        return ticket;
    }
}
