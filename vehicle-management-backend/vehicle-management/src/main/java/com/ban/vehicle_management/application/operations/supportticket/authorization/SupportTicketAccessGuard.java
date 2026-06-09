package com.ban.vehicle_management.application.operations.supportticket.authorization;

import com.ban.vehicle_management.application.iam.account.port.in.CurrentAccountPortIn;
import com.ban.vehicle_management.application.iam.account.port.out.AccountProfilePortOut;
import com.ban.vehicle_management.domain.iam.account.model.AccountProfileState;
import com.ban.vehicle_management.domain.operations.supportticket.model.SupportTicket;
import com.ban.vehicle_management.shared.enumeration.people.CustomerApprovalStatus;
import com.ban.vehicle_management.shared.enumeration.people.CustomerStatus;
import java.util.UUID;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

@Component
public class SupportTicketAccessGuard {

    private static final String CREATE_OWN_PERMISSION = "SUPPORT_TICKET_CREATE_OWN";
    private static final String READ_ALL_PERMISSION = "SUPPORT_TICKET_READ_ALL";
    private static final String READ_OWN_PERMISSION = "SUPPORT_TICKET_READ_OWN";
    private static final String READ_ASSIGNED_PERMISSION = "SUPPORT_TICKET_READ_ASSIGNED";
    private static final String UPDATE_ALL_PERMISSION = "SUPPORT_TICKET_UPDATE_ALL";
    private static final String UPDATE_OWN_PERMISSION = "SUPPORT_TICKET_UPDATE_OWN";
    private static final String ASSIGN_PERMISSION = "SUPPORT_TICKET_ASSIGN";
    private static final String PROCESS_ALL_PERMISSION = "SUPPORT_TICKET_PROCESS_ALL";
    private static final String PROCESS_ASSIGNED_PERMISSION = "SUPPORT_TICKET_PROCESS_ASSIGNED";
    private static final String REOPEN_ALL_PERMISSION = "SUPPORT_TICKET_REOPEN_ALL";
    private static final String REOPEN_OWN_PERMISSION = "SUPPORT_TICKET_REOPEN_OWN";
    private static final String CLOSE_ALL_PERMISSION = "SUPPORT_TICKET_CLOSE_ALL";
    private static final String CLOSE_OWN_PERMISSION = "SUPPORT_TICKET_CLOSE_OWN";

    private final CurrentAccountPortIn currentAccountPortIn;
    private final AccountProfilePortOut accountProfilePortOut;

    public SupportTicketAccessGuard(
            CurrentAccountPortIn currentAccountPortIn,
            AccountProfilePortOut accountProfilePortOut
    ) {
        this.currentAccountPortIn = currentAccountPortIn;
        this.accountProfilePortOut = accountProfilePortOut;
    }

    public UUID resolveCustomerIdForCreate() {
        currentAccountPortIn.requirePermission(CREATE_OWN_PERMISSION);
        return resolveCurrentApprovedCustomerId();
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

        currentAccountPortIn.requirePermission(READ_OWN_PERMISSION);
        UUID customerId = resolveCurrentApprovedCustomerId();
        if (!customerId.equals(ticket.getCustomerId())) {
            throw new AccessDeniedException("Access is denied");
        }
    }

    public void ensureCanUpdate(SupportTicket ticket) {
        if (currentAccountPortIn.hasPermission(UPDATE_ALL_PERMISSION)) {
            return;
        }

        currentAccountPortIn.requirePermission(UPDATE_OWN_PERMISSION);
        UUID customerId = resolveCurrentApprovedCustomerId();
        if (!customerId.equals(ticket.getCustomerId())) {
            throw new AccessDeniedException("Access is denied");
        }
    }

    public void ensureCanAssign() {
        currentAccountPortIn.requirePermission(ASSIGN_PERMISSION);
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

    public void ensureCanReopen(SupportTicket ticket) {
        if (currentAccountPortIn.hasPermission(REOPEN_ALL_PERMISSION)) {
            return;
        }

        currentAccountPortIn.requirePermission(REOPEN_OWN_PERMISSION);
        UUID customerId = resolveCurrentApprovedCustomerId();
        if (!customerId.equals(ticket.getCustomerId())) {
            throw new AccessDeniedException("Access is denied");
        }
    }

    public UUID resolveClosedByForClose(SupportTicket ticket) {
        UUID accountId = currentAccountPortIn.getCurrentAccountIdOrThrow();

        if (currentAccountPortIn.hasPermission(CLOSE_ALL_PERMISSION)) {
            return accountId;
        }

        currentAccountPortIn.requirePermission(CLOSE_OWN_PERMISSION);
        UUID customerId = resolveCurrentApprovedCustomerId();
        if (!customerId.equals(ticket.getCustomerId())) {
            throw new AccessDeniedException("Access is denied");
        }

        return accountId;
    }

    public UUID currentAccountId() {
        return currentAccountPortIn.getCurrentAccountIdOrThrow();
    }

    private UUID resolveCurrentApprovedCustomerId() {
        UUID accountId = currentAccountPortIn.getCurrentAccountIdOrThrow();

        AccountProfileState profileState = accountProfilePortOut.findProfileStateByAccountId(accountId)
                .orElseThrow(() -> new AccessDeniedException("Access is denied"));

        if (profileState.customerId() == null
                || !CustomerStatus.ACTIVE.equals(profileState.customerStatus())
                || !CustomerApprovalStatus.APPROVED.equals(profileState.customerApprovalStatus())) {
            throw new AccessDeniedException("Access is denied");
        }

        return profileState.customerId();
    }
}