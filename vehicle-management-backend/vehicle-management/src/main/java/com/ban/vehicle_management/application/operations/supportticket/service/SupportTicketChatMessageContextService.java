package com.ban.vehicle_management.application.operations.supportticket.service;

import com.ban.vehicle_management.application.iam.account.port.in.CurrentAccountPortIn;
import com.ban.vehicle_management.application.operations.chatconversation.port.out.ChatConversationPortOut;
import com.ban.vehicle_management.application.operations.supportticket.port.out.SupportTicketConversationLinkPortOut;
import com.ban.vehicle_management.application.operations.supportticket.port.out.SupportTicketPortOut;
import com.ban.vehicle_management.domain.operations.chatconversation.model.ChatConversation;
import com.ban.vehicle_management.domain.operations.supportticket.model.SupportTicket;
import com.ban.vehicle_management.domain.operations.supportticket.policy.SupportTicketPolicy;
import com.ban.vehicle_management.shared.enumeration.operations.ChatConversationType;
import com.ban.vehicle_management.shared.enumeration.operations.SupportTicketStatus;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import com.ban.vehicle_management.shared.exception.NotFoundException;
import java.time.Instant;
import java.util.UUID;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

/** Enforces the ticket scope of every customer-staff chat message. */
@Component
public class SupportTicketChatMessageContextService {

    private final CurrentAccountPortIn currentAccountPortIn;
    private final ChatConversationPortOut chatPortOut;
    private final SupportTicketPortOut supportTicketPortOut;
    private final SupportTicketConversationLinkPortOut linkPortOut;
    private final SupportTicketConversationService ticketConversationService;
    private final SupportTicketPolicy ticketPolicy = new SupportTicketPolicy();

    public SupportTicketChatMessageContextService(
            CurrentAccountPortIn currentAccountPortIn,
            ChatConversationPortOut chatPortOut,
            SupportTicketPortOut supportTicketPortOut,
            SupportTicketConversationLinkPortOut linkPortOut,
            SupportTicketConversationService ticketConversationService
    ) {
        this.currentAccountPortIn = currentAccountPortIn;
        this.chatPortOut = chatPortOut;
        this.supportTicketPortOut = supportTicketPortOut;
        this.linkPortOut = linkPortOut;
        this.ticketConversationService = ticketConversationService;
    }

    public void ensureCanSend(ChatConversation conversation, UUID contextTicketId, UUID senderAccountId) {
        if (conversation.getConversationType() != ChatConversationType.CUSTOMER_DIRECT) {
            return;
        }
        if (contextTicketId == null) {
            throw new BadRequestException("contextTicketId is required for customer support chat messages");
        }

        SupportTicket ticket = supportTicketPortOut.findById(contextTicketId)
                .orElseThrow(() -> new NotFoundException("Support ticket not found"));
        if (!ticket.getCustomerId().equals(conversation.getCustomerId())) {
            throw new AccessDeniedException("Access is denied");
        }

        UUID customerAccountId = chatPortOut.findAccountIdByCustomerId(ticket.getCustomerId())
                .orElseThrow(() -> new NotFoundException("Customer account not found"));
        boolean isCustomer = customerAccountId.equals(senderAccountId);
        boolean isCurrentAssignee = senderAccountId.equals(ticket.getAssignedTo());
        if (!isCustomer && !isCurrentAssignee) {
            throw new AccessDeniedException("Access is denied");
        }
        if (ticket.getStatus() == SupportTicketStatus.RESOLVED || ticket.getStatus() == SupportTicketStatus.CLOSED) {
            throw new BadRequestException("Resolved or closed support ticket is read-only");
        }

        boolean linkedToConversation = linkPortOut.findActiveBySupportTicketId(ticket.getSupportTicketId())
                .map(link -> conversation.getConversationId().equals(link.getConversationId()))
                .orElse(false);
        if (!linkedToConversation) {
            throw new AccessDeniedException("Access is denied");
        }

        if (isCurrentAssignee) {
            currentAccountPortIn.requirePermission("SUPPORT_TICKET_READ_ASSIGNED");
            if (!currentAccountPortIn.hasPermission("SUPPORT_TICKET_PROCESS_ALL")) {
                currentAccountPortIn.requirePermission("SUPPORT_TICKET_PROCESS_ASSIGNED");
            }
            currentAccountPortIn.requirePermission("SUPPORT_TICKET_RESPOND_ASSIGNED");
        }

        if (ticket.getStatus() == SupportTicketStatus.OPEN) {
            if (isCustomer) {
                return;
            }
            ticketPolicy.startProgress(ticket);
            if (ticket.getFirstRespondedAt() == null) {
                ticket.setFirstRespondedAt(Instant.now());
            }
            ticket = supportTicketPortOut.save(ticket);
            publishFirstReplyUpdate(ticket);
            return;
        }

        if (ticket.getStatus() != SupportTicketStatus.IN_PROGRESS) {
            throw new BadRequestException("Support ticket is not available for messaging");
        }

    }

    private void publishFirstReplyUpdate(SupportTicket ticket) {
        ticketConversationService.postAssistantTicketUpdate(
                ticket,
                "Nhân viên phụ trách đã phản hồi yêu cầu của bạn. Bạn có thể chọn Trao đổi để tiếp tục hỗ trợ."
        );
    }
}
