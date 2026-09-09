package com.ban.vehicle_management.domain.operations.approvalrequest.policy;

import com.ban.vehicle_management.domain.operations.supportticket.model.SupportTicket;
import com.ban.vehicle_management.shared.enumeration.operations.SupportTicketEscalationDecision;
import com.ban.vehicle_management.shared.enumeration.operations.SupportTicketEscalationReason;
import com.ban.vehicle_management.shared.enumeration.operations.SupportTicketStatus;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import com.ban.vehicle_management.shared.exception.ConflictException;
import com.ban.vehicle_management.shared.utils.TextValidationUtils;
import java.util.UUID;
import org.springframework.security.access.AccessDeniedException;

public class SupportTicketEscalationPolicy {

    public String validateCreate(
            SupportTicket ticket,
            SupportTicketEscalationReason reason,
            String description
    ) {
        if (ticket.getStatus() != SupportTicketStatus.OPEN
                && ticket.getStatus() != SupportTicketStatus.IN_PROGRESS) {
            throw new ConflictException("Only open or in-progress support tickets can be escalated");
        }
        if (ticket.getAssignedTo() == null) {
            throw new ConflictException("The support ticket has not been assigned yet");
        }
        if (reason == null) {
            throw new BadRequestException("reasonCode must not be null");
        }
        return TextValidationUtils.normalizeRequiredText(description, "description", 1000);
    }

    public String validateReview(
            SupportTicket ticket,
            UUID reviewerAccountId,
            SupportTicketEscalationDecision decision,
            UUID assignedTo,
            String note
    ) {
        ensureReviewerIsIndependent(ticket, reviewerAccountId);
        if (decision == null) {
            throw new BadRequestException("decision must not be null");
        }
        if (decision == SupportTicketEscalationDecision.REASSIGN) {
            if (assignedTo == null) {
                throw new BadRequestException("assignedTo is required for reassignment");
            }
            if (assignedTo.equals(ticket.getAssignedTo())) {
                throw new ConflictException("The selected account is already assigned to the support ticket");
            }
        } else if (assignedTo != null) {
            throw new BadRequestException("assignedTo is only accepted for reassignment");
        }
        return TextValidationUtils.normalizeRequiredText(note, "note", 500);
    }

    public String validateRejection(SupportTicket ticket, UUID reviewerAccountId, String note) {
        ensureReviewerIsIndependent(ticket, reviewerAccountId);
        return TextValidationUtils.normalizeRequiredText(note, "note", 500);
    }

    private void ensureReviewerIsIndependent(SupportTicket ticket, UUID reviewerAccountId) {
        if (reviewerAccountId.equals(ticket.getAssignedTo())) {
            throw new AccessDeniedException("The assigned employee cannot review their own escalation");
        }
    }
}
