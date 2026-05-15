package com.ban.vehicle_management.domain.operations.supportticket.policy;

import com.ban.vehicle_management.domain.operations.supportticket.model.SupportTicket;
import com.ban.vehicle_management.shared.enumeration.SupportTicketPriority;
import com.ban.vehicle_management.shared.enumeration.SupportTicketStatus;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import java.time.Instant;
import java.util.UUID;

public class SupportTicketPolicy {

    public void initialize(SupportTicket supportTicket) {
        requireSupportTicket(supportTicket);
        supportTicket.setTitle(normalizeRequired(supportTicket.getTitle(), "title"));
        supportTicket.setContent(normalizeRequired(supportTicket.getContent(), "content"));
        if (supportTicket.getStatus() == null) {
            supportTicket.setStatus(SupportTicketStatus.OPEN);
        }
        if (supportTicket.getPriority() == null) {
            supportTicket.setPriority(SupportTicketPriority.NORMAL);
        }
        validateState(supportTicket);
    }

    public void assign(SupportTicket supportTicket, UUID assignedTo) {
        requireSupportTicket(supportTicket);
        requireField(assignedTo, "assignedTo");
        if (supportTicket.getStatus() == SupportTicketStatus.RESOLVED || supportTicket.getStatus() == SupportTicketStatus.CLOSED) {
            throw new BadRequestException("Resolved or closed ticket cannot be reassigned");
        }

        supportTicket.setAssignedTo(assignedTo);
        validateState(supportTicket);
    }

    public void startProgress(SupportTicket supportTicket) {
        requireSupportTicket(supportTicket);
        if (supportTicket.getStatus() != SupportTicketStatus.OPEN) {
            throw new BadRequestException("Support ticket must be OPEN to start progress");
        }
        if (supportTicket.getAssignedTo() == null) {
            throw new BadRequestException("Support ticket must be assigned before starting progress");
        }

        supportTicket.setStatus(SupportTicketStatus.IN_PROGRESS);
        validateState(supportTicket);
    }

    public void resolve(SupportTicket supportTicket, Instant resolvedAt) {
        requireSupportTicket(supportTicket);
        if (supportTicket.getStatus() != SupportTicketStatus.OPEN && supportTicket.getStatus() != SupportTicketStatus.IN_PROGRESS) {
            throw new BadRequestException("Support ticket can only be resolved from OPEN or IN_PROGRESS status");
        }
        requireField(resolvedAt, "resolvedAt");

        supportTicket.setStatus(SupportTicketStatus.RESOLVED);
        supportTicket.setResolvedAt(resolvedAt);
        validateState(supportTicket);
    }

    public void close(SupportTicket supportTicket) {
        requireSupportTicket(supportTicket);
        if (supportTicket.getStatus() != SupportTicketStatus.RESOLVED) {
            throw new BadRequestException("Only resolved ticket can be closed");
        }

        supportTicket.setStatus(SupportTicketStatus.CLOSED);
        validateState(supportTicket);
    }

    public void validateState(SupportTicket supportTicket) {
        requireSupportTicket(supportTicket);
        supportTicket.setTitle(normalizeRequired(supportTicket.getTitle(), "title"));
        supportTicket.setContent(normalizeRequired(supportTicket.getContent(), "content"));
        requireField(supportTicket.getStatus(), "status");
        requireField(supportTicket.getPriority(), "priority");

        if (supportTicket.getStatus() == SupportTicketStatus.IN_PROGRESS && supportTicket.getAssignedTo() == null) {
            throw new BadRequestException("In-progress ticket must have assignedTo");
        }

        if (supportTicket.getStatus() == SupportTicketStatus.RESOLVED || supportTicket.getStatus() == SupportTicketStatus.CLOSED) {
            requireField(supportTicket.getResolvedAt(), "resolvedAt");
            return;
        }

        if (supportTicket.getResolvedAt() != null) {
            throw new BadRequestException("Only resolved or closed ticket can keep resolvedAt");
        }
    }

    private void requireSupportTicket(SupportTicket supportTicket) {
        requireField(supportTicket, "supportTicket");
    }

    private void requireField(Object value, String fieldName) {
        if (value == null) {
            throw new BadRequestException(fieldName + " must not be null");
        }
    }

    private String normalizeRequired(String value, String fieldName) {
        String normalizedValue = normalizeNullable(value);
        if (normalizedValue == null) {
            throw new BadRequestException(fieldName + " must not be blank");
        }
        return normalizedValue;
    }

    private String normalizeNullable(String value) {
        if (value == null) {
            return null;
        }

        String normalizedValue = value.trim();
        return normalizedValue.isEmpty() ? null : normalizedValue;
    }
}

