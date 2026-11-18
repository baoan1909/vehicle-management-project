package com.ban.vehicle_management.application.operations.supportticket.authorization;

import com.ban.vehicle_management.application.iam.account.port.in.CurrentAccountPortIn;
import com.ban.vehicle_management.application.operations.supportticket.model.SupportRequesterContext;
import com.ban.vehicle_management.domain.operations.supportticket.model.SupportTicket;
import com.ban.vehicle_management.shared.enumeration.operations.SupportTicketStatus;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import java.util.UUID;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

@Component
public class SupportTicketAccessGuard {

    private final CurrentAccountPortIn currentAccountPortIn;
    private final SupportRequesterContextResolver requesterContextResolver;

    public SupportTicketAccessGuard(
            CurrentAccountPortIn currentAccountPortIn,
            SupportRequesterContextResolver requesterContextResolver
    ) {
        this.currentAccountPortIn = currentAccountPortIn;
        this.requesterContextResolver = requesterContextResolver;
    }

    public SupportRequesterContext requesterForCreate() {
        return requesterContextResolver.resolve("SUPPORT_TICKET_CREATE_OWN");
    }

    public SupportRequesterContext requesterForAssistant() {
        return requesterContextResolver.resolve("SUPPORT_WIDGET_ACCESS_OWN");
    }

    public SupportRequesterContext requesterForCreateFromChat() {
        return requesterContextResolver.resolve("SUPPORT_TICKET_CREATE_FROM_CHAT_OWN");
    }

    public SupportRequesterContext requesterForOwnTickets() {
        return requesterContextResolver.resolve("SUPPORT_TICKET_READ_OWN");
    }

    public void ensureRequesterScope(SupportRequesterContext requester, UUID organizationId, UUID parkingLotId) {
        requesterContextResolver.ensureScopeAllowed(requester, organizationId, parkingLotId);
    }

    public void ensureCanRead(SupportTicket ticket) {
        if (!currentAccountPortIn.hasPermission("SUPPORT_TICKET_READ_ALL")) {
            ensureOwner(ticket, "SUPPORT_TICKET_READ_OWN");
        }
    }

    public void ensureCanUpdate(SupportTicket ticket) {
        if (!currentAccountPortIn.hasPermission("SUPPORT_TICKET_UPDATE_ALL")) {
            ensureOwner(ticket, "SUPPORT_TICKET_UPDATE_OWN");
        }
    }

    public void ensureCanAssign() {
        currentAccountPortIn.requirePermission("SUPPORT_TICKET_ASSIGN");
    }

    public UUID ensureCanCreateOrReadOwnEscalation(SupportTicket ticket) {
        return ensureOwner(ticket, "SUPPORT_TICKET_ESCALATION_CREATE_OWN");
    }

    public UUID ensureCanReviewEscalation() {
        currentAccountPortIn.requirePermission("SUPPORT_TICKET_ESCALATION_REVIEW_ALL");
        currentAccountPortIn.requirePermission("SUPPORT_TICKET_ASSIGN");
        currentAccountPortIn.requirePermission("SUPPORT_TICKET_READ_ALL");
        return currentAccountId();
    }

    public UUID ensureCanReadOwnRequesterConversation(SupportTicket ticket) {
        return ensureOwner(ticket, "SUPPORT_TICKET_READ_OWN");
    }

    public UUID resolveAccountIdForClaim() {
        currentAccountPortIn.requirePermission("SUPPORT_TICKET_CLAIM_OWN");
        currentAccountPortIn.requirePermission("SUPPORT_TICKET_PROCESS_ALL");
        return currentAccountId();
    }

    public void ensureCanProcess(SupportTicket ticket) {
        currentAccountPortIn.requirePermission("SUPPORT_TICKET_PROCESS_ALL");
        UUID accountId = currentAccountId();
        if (ticket.getAssignedTo() == null || !accountId.equals(ticket.getAssignedTo())) {
            throw new AccessDeniedException("Support ticket must be claimed by the current authorized handler");
        }
    }

    public UUID ensureCanReplyAsAssignee(SupportTicket ticket) {
        if (ticket.getStatus() != SupportTicketStatus.OPEN
                && ticket.getStatus() != SupportTicketStatus.IN_PROGRESS) {
            throw new BadRequestException("Only open or in-progress support ticket can be replied to");
        }
        ensureCanProcess(ticket);
        currentAccountPortIn.requirePermission("SUPPORT_TICKET_RESPOND_ASSIGNED");
        return currentAccountId();
    }

    public UUID ensureCanShareAsAssignee(SupportTicket ticket) {
        ensureCanProcess(ticket);
        currentAccountPortIn.requirePermission("SUPPORT_TICKET_RESPOND_ASSIGNED");
        return currentAccountId();
    }

    public void ensureCanReopen(SupportTicket ticket) {
        if (!currentAccountPortIn.hasPermission("SUPPORT_TICKET_REOPEN_ALL")) {
            ensureOwner(ticket, "SUPPORT_TICKET_REOPEN_OWN");
        }
    }

    public UUID resolveClosedByForClose(SupportTicket ticket) {
        if (currentAccountPortIn.hasPermission("SUPPORT_TICKET_CLOSE_ALL")) {
            return currentAccountId();
        }
        return ensureOwner(ticket, "SUPPORT_TICKET_CLOSE_OWN");
    }

    public UUID currentAccountId() {
        return currentAccountPortIn.getCurrentAccountIdOrThrow();
    }

    private UUID ensureOwner(SupportTicket ticket, String permission) {
        SupportRequesterContext requester = requesterContextResolver.resolve(permission);
        if (!requester.accountId().equals(ticket.getRequesterAccountId())) {
            throw new AccessDeniedException("Access is denied");
        }
        return requester.accountId();
    }
}
