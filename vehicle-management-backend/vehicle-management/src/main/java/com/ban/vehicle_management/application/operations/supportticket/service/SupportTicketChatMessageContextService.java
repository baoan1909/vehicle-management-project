package com.ban.vehicle_management.application.operations.supportticket.service;

import com.ban.vehicle_management.application.iam.account.port.in.CurrentAccountPortIn;
import com.ban.vehicle_management.application.operations.supportticket.port.out.SupportTicketConversationLinkPortOut;
import com.ban.vehicle_management.application.operations.supportticket.port.out.SupportTicketPortOut;
import com.ban.vehicle_management.domain.operations.chatconversation.model.ChatConversation;
import com.ban.vehicle_management.domain.operations.supportticket.model.SupportTicket;
import com.ban.vehicle_management.domain.operations.supportticket.policy.SupportTicketPolicy;
import com.ban.vehicle_management.shared.enumeration.operations.ChatConversationType;
import com.ban.vehicle_management.shared.enumeration.operations.SupportTicketStatus;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** Normalizes optional ticket context without preventing an authorized private chat message. */
@Component
public class SupportTicketChatMessageContextService {

    private final CurrentAccountPortIn currentAccountPortIn;
    private final SupportTicketPortOut supportTicketPortOut;
    private final SupportTicketConversationLinkPortOut linkPortOut;
    private final SupportTicketConversationService ticketConversationService;
    private final SupportTicketPolicy ticketPolicy = new SupportTicketPolicy();

    public SupportTicketChatMessageContextService(
            CurrentAccountPortIn currentAccountPortIn,
            SupportTicketPortOut supportTicketPortOut,
            SupportTicketConversationLinkPortOut linkPortOut,
            SupportTicketConversationService ticketConversationService
    ) {
        this.currentAccountPortIn = currentAccountPortIn;
        this.supportTicketPortOut = supportTicketPortOut;
        this.linkPortOut = linkPortOut;
        this.ticketConversationService = ticketConversationService;
    }

    public UUID resolveContextForSend(ChatConversation conversation, UUID contextTicketId, UUID senderAccountId) {
        if (conversation.getConversationType() != ChatConversationType.SUPPORT_REQUESTER_DIRECT) {
            return contextTicketId;
        }
        if (contextTicketId == null) {
            // A requester-to-handler conversation is also a private customer-care channel.
            // Ticket-specific authorization and lifecycle changes only apply when the sender
            // explicitly attaches a ticket context. Active membership and send permission
            // are enforced by ChatConversationUseCaseImpl before this hook is invoked.
            return null;
        }

        SupportTicket ticket = supportTicketPortOut.findByIdForUpdate(contextTicketId).orElse(null);
        if (ticket == null) {
            return null;
        }
        if (!ticket.getRequesterAccountId().equals(conversation.getOwnerAccountId())) {
            return null;
        }

        if (ticket.getStatus() == SupportTicketStatus.CLOSED) {
            // The private conversation remains available after the ticket lifecycle ends.
            // A stale browser may still submit the closed ticket context, so detach that context
            // and persist the message as general customer-care chat instead of rejecting it.
            return null;
        }

        boolean isRequester = ticket.getRequesterAccountId().equals(senderAccountId);
        boolean isCurrentAssignee = senderAccountId.equals(ticket.getAssignedTo());
        if (!isRequester && !isCurrentAssignee) {
            return null;
        }

        boolean linkedToConversation = linkPortOut.findActiveBySupportTicketId(ticket.getSupportTicketId())
                .map(link -> conversation.getConversationId().equals(link.getConversationId()))
                .orElse(false);
        if (!linkedToConversation) {
            return null;
        }

        if (isCurrentAssignee && !canRespondToAssignedTicket()) {
            return null;
        }

        if (ticket.getStatus() == SupportTicketStatus.OPEN) {
            if (isRequester) {
                recordActivity(ticket);
                return contextTicketId;
            }
            ticketPolicy.startProgress(ticket);
            if (ticket.getFirstRespondedAt() == null) {
                ticket.setFirstRespondedAt(Instant.now());
            }
            ticketPolicy.recordContextActivity(ticket, Instant.now());
            ticket = supportTicketPortOut.save(ticket);
            publishFirstReplyUpdate(ticket);
            return contextTicketId;
        }

        if (ticket.getStatus() != SupportTicketStatus.IN_PROGRESS
                && ticket.getStatus() != SupportTicketStatus.RESOLVED) {
            return null;
        }

        recordActivity(ticket);
        return contextTicketId;
    }

    private void recordActivity(SupportTicket ticket) {
        ticketPolicy.recordContextActivity(ticket, Instant.now());
        supportTicketPortOut.save(ticket);
    }

    private boolean canRespondToAssignedTicket() {
        return currentAccountPortIn.hasPermission("SUPPORT_TICKET_READ_ALL")
                && currentAccountPortIn.hasPermission("SUPPORT_TICKET_PROCESS_ALL")
                && currentAccountPortIn.hasPermission("SUPPORT_TICKET_RESPOND_ASSIGNED");
    }

    private void publishFirstReplyUpdate(SupportTicket ticket) {
        ticketConversationService.postAssistantTicketUpdate(
                ticket,
                "Nhân viên phụ trách đã phản hồi yêu cầu của bạn. Bạn có thể chọn Trao đổi để tiếp tục hỗ trợ."
        );
    }
}
