package com.ban.vehicle_management.domain.operations.supportticket.policy;

import com.ban.vehicle_management.domain.operations.supportticket.model.SupportTicket;
import com.ban.vehicle_management.shared.enumeration.operations.SupportTicketStatus;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import com.ban.vehicle_management.shared.utils.TextValidationUtils;
import java.time.Instant;
import java.util.UUID;

public class SupportTicketPolicy {

    public void initialize(SupportTicket ticket) {
        requireTicket(ticket);

        if (ticket.getStatus() == null) {
            ticket.setStatus(SupportTicketStatus.OPEN);
        }

        if (ticket.getReopenCount() == null) {
            ticket.setReopenCount(0);
        }

        ticket.setAssignedTo(null);
        ticket.setResolvedAt(null);
        ticket.setResolutionNote(null);
        ticket.setClosedAt(null);
        ticket.setClosedBy(null);
        ticket.setLastReopenedAt(null);

        validateState(ticket);
    }

    public void assign(SupportTicket ticket, UUID assignedTo) {
        requireTicket(ticket);
        requireField(assignedTo, "assignedTo");

        if (ticket.getStatus() != SupportTicketStatus.OPEN
                && ticket.getStatus() != SupportTicketStatus.IN_PROGRESS) {
            throw new BadRequestException("Support ticket can only be assigned from OPEN or IN_PROGRESS status");
        }

        ticket.setAssignedTo(assignedTo);
        validateState(ticket);
    }

    public void startProgress(SupportTicket ticket) {
        requireTicket(ticket);

        if (ticket.getStatus() != SupportTicketStatus.OPEN) {
            throw new BadRequestException("Support ticket must be OPEN to start progress");
        }

        requireField(ticket.getAssignedTo(), "assignedTo");

        ticket.setStatus(SupportTicketStatus.IN_PROGRESS);
        validateState(ticket);
    }

    public void resolve(SupportTicket ticket, String resolutionNote, Instant resolvedAt) {
        requireTicket(ticket);

        if (ticket.getStatus() != SupportTicketStatus.IN_PROGRESS) {
            throw new BadRequestException("Support ticket must be IN_PROGRESS before it can be resolved");
        }

        ticket.setStatus(SupportTicketStatus.RESOLVED);
        ticket.setResolvedAt(requireInstant(resolvedAt, "resolvedAt"));
        ticket.setResolutionNote(TextValidationUtils.normalizeRequiredText(resolutionNote, "resolutionNote", 0));

        validateState(ticket);
    }

    public void reopen(SupportTicket ticket, Instant reopenedAt) {
        requireTicket(ticket);

        if (ticket.getStatus() != SupportTicketStatus.RESOLVED) {
            throw new BadRequestException("Only resolved ticket can be reopened");
        }

        if (ticket.getAssignedTo() == null) {
            ticket.setStatus(SupportTicketStatus.OPEN);
        } else {
            ticket.setStatus(SupportTicketStatus.IN_PROGRESS);
        }
        ticket.setResolvedAt(null);
        ticket.setResolutionNote(null);
        ticket.setReopenCount(ticket.getReopenCount() == null ? 1 : ticket.getReopenCount() + 1);
        ticket.setLastReopenedAt(requireInstant(reopenedAt, "lastReopenedAt"));

        validateState(ticket);
    }

    public void close(SupportTicket ticket, UUID closedBy, Instant closedAt) {
        requireTicket(ticket);

        if (ticket.getStatus() != SupportTicketStatus.RESOLVED) {
            throw new BadRequestException("Only resolved support ticket can be closed");
        }

        requireField(closedBy, "closedBy");

        ticket.setStatus(SupportTicketStatus.CLOSED);
        ticket.setClosedBy(closedBy);
        ticket.setClosedAt(requireInstant(closedAt, "closedAt"));

        validateState(ticket);
    }

    public void validateState(SupportTicket ticket) {
        requireTicket(ticket);

        requireField(ticket.getCustomerId(), "customerId");
        requireField(ticket.getCategoryId(), "categoryId");

        ticket.setTitle(TextValidationUtils.normalizeRequiredText(ticket.getTitle(), "title", 150));
        ticket.setContent(TextValidationUtils.normalizeRequiredText(ticket.getContent(), "content", 0));

        requireField(ticket.getStatus(), "status");

        if (ticket.getReopenCount() == null || ticket.getReopenCount() < 0) {
            throw new BadRequestException("reopenCount must not be negative");
        }

        if (ticket.getStatus() == SupportTicketStatus.IN_PROGRESS) {
            requireField(ticket.getAssignedTo(), "assignedTo");
        }

        if (ticket.getStatus() == SupportTicketStatus.RESOLVED) {
            requireField(ticket.getResolvedAt(), "resolvedAt");
            ticket.setResolutionNote(TextValidationUtils.normalizeRequiredText(ticket.getResolutionNote(), "resolutionNote", 0));
        }

        if (ticket.getStatus() == SupportTicketStatus.CLOSED) {
            requireField(ticket.getClosedAt(), "closedAt");
            requireField(ticket.getClosedBy(), "closedBy");
        }

        if ((ticket.getStatus() == SupportTicketStatus.OPEN || ticket.getStatus() == SupportTicketStatus.IN_PROGRESS)
                && ticket.getResolvedAt() != null) {
            throw new BadRequestException("Unresolved ticket must not keep resolvedAt");
        }

        if (ticket.getStatus() != SupportTicketStatus.CLOSED && ticket.getClosedAt() != null) {
            throw new BadRequestException("Unclosed ticket must not keep closedAt");
        }

        if (ticket.getStatus() != SupportTicketStatus.CLOSED && ticket.getClosedBy() != null) {
            throw new BadRequestException("Unclosed ticket must not keep closedBy");
        }
    }

    private void requireTicket(SupportTicket ticket) {
        requireField(ticket, "supportTicket");
    }

    private Instant requireInstant(Instant value, String fieldName) {
        requireField(value, fieldName);
        return value;
    }

    private void requireField(Object value, String fieldName) {
        if (value == null) {
            throw new BadRequestException(fieldName + " must not be null");
        }
    }
}
