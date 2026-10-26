package com.ban.vehicle_management.application.operations.supportticket.authorization;

import com.ban.vehicle_management.application.iam.account.port.in.CurrentAccountPortIn;
import com.ban.vehicle_management.application.people.customer.authorization.CustomerIdentityResolver;
import com.ban.vehicle_management.domain.operations.supportticket.model.SupportTicket;
import com.ban.vehicle_management.shared.enumeration.operations.SupportTicketStatus;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import java.util.UUID;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

@Component
public class SupportTicketAccessGuard {

    private static final String CREATE_OWN_PERMISSION = "SUPPORT_TICKET_CREATE_OWN";
    private static final String ASSISTANT_ACCESS_OWN_PERMISSION = "SUPPORT_WIDGET_ACCESS_OWN";
    private static final String CREATE_FROM_CHAT_OWN_PERMISSION = "SUPPORT_TICKET_CREATE_FROM_CHAT_OWN";
    private static final String READ_ALL_PERMISSION = "SUPPORT_TICKET_READ_ALL";
    private static final String READ_OWN_PERMISSION = "SUPPORT_TICKET_READ_OWN";
    private static final String READ_ASSIGNED_PERMISSION = "SUPPORT_TICKET_READ_ASSIGNED";
    private static final String UPDATE_ALL_PERMISSION = "SUPPORT_TICKET_UPDATE_ALL";
    private static final String UPDATE_OWN_PERMISSION = "SUPPORT_TICKET_UPDATE_OWN";
    private static final String ASSIGN_PERMISSION = "SUPPORT_TICKET_ASSIGN";
    private static final String CLAIM_OWN_PERMISSION = "SUPPORT_TICKET_CLAIM_OWN";
    private static final String PROCESS_ALL_PERMISSION = "SUPPORT_TICKET_PROCESS_ALL";
    private static final String PROCESS_ASSIGNED_PERMISSION = "SUPPORT_TICKET_PROCESS_ASSIGNED";
    private static final String RESPOND_ASSIGNED_PERMISSION = "SUPPORT_TICKET_RESPOND_ASSIGNED";
    private static final String REOPEN_ALL_PERMISSION = "SUPPORT_TICKET_REOPEN_ALL";
    private static final String REOPEN_OWN_PERMISSION = "SUPPORT_TICKET_REOPEN_OWN";
    private static final String CLOSE_ALL_PERMISSION = "SUPPORT_TICKET_CLOSE_ALL";
    private static final String CLOSE_OWN_PERMISSION = "SUPPORT_TICKET_CLOSE_OWN";
    private static final String ESCALATION_CREATE_OWN_PERMISSION = "SUPPORT_TICKET_ESCALATION_CREATE_OWN";
    private static final String ESCALATION_REVIEW_ALL_PERMISSION = "SUPPORT_TICKET_ESCALATION_REVIEW_ALL";

    private final CurrentAccountPortIn currentAccountPortIn;
    private final CustomerIdentityResolver customerIdentityResolver;

    public SupportTicketAccessGuard(
            CurrentAccountPortIn currentAccountPortIn,
            CustomerIdentityResolver customerIdentityResolver
    ) {
        this.currentAccountPortIn = currentAccountPortIn;
        this.customerIdentityResolver = customerIdentityResolver;
    }

    public UUID resolveCustomerIdForCreate() {
        return resolveCurrentSupportCustomerId(CREATE_OWN_PERMISSION);
    }

    public UUID resolveCustomerIdForAssistant() {
        return resolveCurrentSupportCustomerId(ASSISTANT_ACCESS_OWN_PERMISSION);
    }

    public UUID resolveCustomerIdForCreateFromChat() {
        return resolveCurrentSupportCustomerId(CREATE_FROM_CHAT_OWN_PERMISSION);
    }

    public UUID resolveCustomerIdForOwnTickets() {
        return resolveCurrentSupportCustomerId(READ_OWN_PERMISSION);
    }

    public void ensureCanRead(SupportTicket ticket) {
        if (currentAccountPortIn.hasPermission(READ_ALL_PERMISSION)) {
            return;
        }

        UUID accountId = currentAccountPortIn.getCurrentAccountIdOrThrow();

        if (currentAccountPortIn.hasPermission(READ_ASSIGNED_PERMISSION)
                && accountId.equals(ticket.getAssignedTo())) {
            return;
        }

        UUID customerId = resolveCurrentSupportCustomerId(READ_OWN_PERMISSION);
        if (!customerId.equals(ticket.getCustomerId())) {
            throw new AccessDeniedException("Access is denied");
        }
    }

    public void ensureCanUpdate(SupportTicket ticket) {
        if (currentAccountPortIn.hasPermission(UPDATE_ALL_PERMISSION)) {
            return;
        }

        UUID customerId = resolveCurrentSupportCustomerId(UPDATE_OWN_PERMISSION);
        if (!customerId.equals(ticket.getCustomerId())) {
            throw new AccessDeniedException("Access is denied");
        }
    }

    public void ensureCanAssign() {
        currentAccountPortIn.requirePermission(ASSIGN_PERMISSION);
    }

    public UUID ensureCanCreateOrReadOwnEscalation(SupportTicket ticket) {
        UUID customerId = resolveCurrentSupportCustomerId(ESCALATION_CREATE_OWN_PERMISSION);
        if (!customerId.equals(ticket.getCustomerId())) {
            throw new AccessDeniedException("Access is denied");
        }
        return currentAccountPortIn.getCurrentAccountIdOrThrow();
    }

    public UUID ensureCanReviewEscalation() {
        currentAccountPortIn.requirePermission(ESCALATION_REVIEW_ALL_PERMISSION);
        currentAccountPortIn.requirePermission(ASSIGN_PERMISSION);
        currentAccountPortIn.requirePermission(READ_ALL_PERMISSION);
        return currentAccountPortIn.getCurrentAccountIdOrThrow();
    }

    public UUID ensureCanReadOwnCustomerConversation(SupportTicket ticket) {
        UUID customerId = resolveCurrentSupportCustomerId(READ_OWN_PERMISSION);
        if (!customerId.equals(ticket.getCustomerId())) {
            throw new AccessDeniedException("Access is denied");
        }
        return currentAccountPortIn.getCurrentAccountIdOrThrow();
    }

    public UUID resolveAccountIdForClaim() {
        currentAccountPortIn.requirePermission(CLAIM_OWN_PERMISSION);
        return currentAccountPortIn.getCurrentAccountIdOrThrow();
    }

    public void ensureCanProcess(SupportTicket ticket) {
        if (currentAccountPortIn.hasPermission(PROCESS_ALL_PERMISSION)) {
            return;
        }

        currentAccountPortIn.requirePermission(PROCESS_ASSIGNED_PERMISSION);
        UUID accountId = currentAccountPortIn.getCurrentAccountIdOrThrow();
        if (!accountId.equals(ticket.getAssignedTo())) {
            throw new AccessDeniedException("Access is denied");
        }
    }

    /** A customer-facing reply is issued only by the employee currently assigned to the ticket. */
    public UUID ensureCanReplyAsAssignee(SupportTicket ticket) {
        if (ticket.getStatus() != SupportTicketStatus.OPEN && ticket.getStatus() != SupportTicketStatus.IN_PROGRESS) {
            throw new BadRequestException("Only open or in-progress support ticket can be replied to");
        }
        UUID accountId = currentAccountPortIn.getCurrentAccountIdOrThrow();
        if (!accountId.equals(ticket.getAssignedTo())) {
            throw new AccessDeniedException("Access is denied");
        }
        currentAccountPortIn.requirePermission(READ_ASSIGNED_PERMISSION);
        if (!currentAccountPortIn.hasPermission(PROCESS_ALL_PERMISSION)) {
            currentAccountPortIn.requirePermission(PROCESS_ASSIGNED_PERMISSION);
        }
        currentAccountPortIn.requirePermission(RESPOND_ASSIGNED_PERMISSION);
        currentAccountPortIn.requirePermission("CHAT_CONVERSATION_CREATE_CUSTOMER_DIRECT");
        return accountId;
    }

    public void ensureCanReopen(SupportTicket ticket) {
        if (currentAccountPortIn.hasPermission(REOPEN_ALL_PERMISSION)) {
            return;
        }

        UUID customerId = resolveCurrentSupportCustomerId(REOPEN_OWN_PERMISSION);
        if (!customerId.equals(ticket.getCustomerId())) {
            throw new AccessDeniedException("Access is denied");
        }
    }

    public UUID resolveClosedByForClose(SupportTicket ticket) {
        UUID accountId = currentAccountPortIn.getCurrentAccountIdOrThrow();

        if (currentAccountPortIn.hasPermission(CLOSE_ALL_PERMISSION)) {
            return accountId;
        }

        UUID customerId = resolveCurrentSupportCustomerId(CLOSE_OWN_PERMISSION);
        if (!customerId.equals(ticket.getCustomerId())) {
            throw new AccessDeniedException("Access is denied");
        }

        return accountId;
    }

    public UUID currentAccountId() {
        return currentAccountPortIn.getCurrentAccountIdOrThrow();
    }

    private UUID resolveCurrentSupportCustomerId(String permissionCode) {
        return customerIdentityResolver.resolveCurrentCustomerIdForSupport(permissionCode);
    }
}
