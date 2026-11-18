package com.ban.vehicle_management.domain.operations.supportticket.policy;

import com.ban.vehicle_management.domain.operations.supportticket.model.SupportTicket;
import com.ban.vehicle_management.shared.enumeration.operations.SupportTicketCloseReason;
import com.ban.vehicle_management.shared.enumeration.operations.SupportTicketStatus;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import com.ban.vehicle_management.shared.utils.TextValidationUtils;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

public class SupportTicketPolicy {

    public static final Duration AUTO_CLOSE_INACTIVITY = Duration.ofHours(24);

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
        ticket.setLastContextActivityAt(null);
        ticket.setAutoCloseAt(null);
        ticket.setClosedAt(null);
        ticket.setClosedBy(null);
        ticket.setCloseReason(null);
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

    public void claim(SupportTicket ticket, UUID accountId) {
        requireTicket(ticket);
        requireField(accountId, "accountId");
        if (ticket.getStatus() != SupportTicketStatus.OPEN || ticket.getAssignedTo() != null) {
            throw new BadRequestException("Only an unassigned open support ticket can be claimed");
        }
        ticket.setAssignedTo(accountId);
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
        Instant normalizedResolvedAt = requireInstant(resolvedAt, "resolvedAt");
        ticket.setResolvedAt(normalizedResolvedAt);
        ticket.setAutoCloseAt(normalizedResolvedAt.plus(AUTO_CLOSE_INACTIVITY));
        // RESOLVED is a workflow flag. A separate result note is intentionally optional.
        ticket.setResolutionNote(null);

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
        ticket.setAutoCloseAt(null);
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
        ticket.setCloseReason(SupportTicketCloseReason.MANUAL);
        ticket.setAutoCloseAt(null);

        validateState(ticket);
    }

    public void autoCloseForInactivity(SupportTicket ticket, Instant closedAt) {
        requireTicket(ticket);
        Instant normalizedClosedAt = requireInstant(closedAt, "closedAt");
        if (ticket.getStatus() != SupportTicketStatus.RESOLVED) {
            throw new BadRequestException("Only resolved support ticket can be closed automatically");
        }
        if (ticket.getAutoCloseAt() == null || ticket.getAutoCloseAt().isAfter(normalizedClosedAt)) {
            throw new BadRequestException("Support ticket inactivity deadline has not elapsed");
        }

        ticket.setStatus(SupportTicketStatus.CLOSED);
        ticket.setClosedBy(null);
        ticket.setClosedAt(normalizedClosedAt);
        ticket.setCloseReason(SupportTicketCloseReason.INACTIVITY);
        ticket.setAutoCloseAt(null);

        validateState(ticket);
    }

    public void recordContextActivity(SupportTicket ticket, Instant activityAt) {
        requireTicket(ticket);
        Instant normalizedActivityAt = requireInstant(activityAt, "activityAt");
        if (ticket.getStatus() == SupportTicketStatus.CLOSED) {
            throw new BadRequestException("Closed support ticket cannot receive contextual activity");
        }

        Instant currentActivityAt = ticket.getLastContextActivityAt();
        if (currentActivityAt == null || normalizedActivityAt.isAfter(currentActivityAt)) {
            ticket.setLastContextActivityAt(normalizedActivityAt);
        }
        if (ticket.getStatus() == SupportTicketStatus.RESOLVED) {
            Instant baseline = normalizedActivityAt.isAfter(ticket.getResolvedAt())
                    ? normalizedActivityAt
                    : ticket.getResolvedAt();
            Instant deadline = baseline.plus(AUTO_CLOSE_INACTIVITY);
            if (ticket.getAutoCloseAt() == null || deadline.isAfter(ticket.getAutoCloseAt())) {
                ticket.setAutoCloseAt(deadline);
            }
        }

        validateState(ticket);
    }

    public void validateState(SupportTicket ticket) {
        requireTicket(ticket);

        requireField(ticket.getRequesterAccountId(), "requesterAccountId");
        requireField(ticket.getRequesterRoleCode(), "requesterRoleCode");
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
            requireField(ticket.getAutoCloseAt(), "autoCloseAt");
        }

        if (ticket.getStatus() == SupportTicketStatus.CLOSED) {
            requireField(ticket.getClosedAt(), "closedAt");
            requireField(ticket.getCloseReason(), "closeReason");
            if (ticket.getCloseReason() == SupportTicketCloseReason.MANUAL) {
                requireField(ticket.getClosedBy(), "closedBy");
            }
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

        if (ticket.getStatus() != SupportTicketStatus.CLOSED && ticket.getCloseReason() != null) {
            throw new BadRequestException("Unclosed ticket must not keep closeReason");
        }

        if (ticket.getStatus() != SupportTicketStatus.RESOLVED && ticket.getAutoCloseAt() != null) {
            throw new BadRequestException("Only resolved ticket can keep autoCloseAt");
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
