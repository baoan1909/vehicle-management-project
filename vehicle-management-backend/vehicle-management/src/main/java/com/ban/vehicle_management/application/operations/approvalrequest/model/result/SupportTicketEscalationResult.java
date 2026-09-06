package com.ban.vehicle_management.application.operations.approvalrequest.model.result;

import com.ban.vehicle_management.shared.enumeration.operations.ApprovalRequestStatus;
import com.ban.vehicle_management.shared.enumeration.operations.SupportTicketEscalationDecision;
import com.ban.vehicle_management.shared.enumeration.operations.SupportTicketEscalationReason;
import java.time.Instant;
import java.util.UUID;

public record SupportTicketEscalationResult(
        UUID escalationId,
        UUID supportTicketId,
        ApprovalRequestStatus status,
        SupportTicketEscalationReason reasonCode,
        String description,
        UUID requestedBy,
        UUID currentAssigneeId,
        Instant requestedAt,
        UUID reviewedBy,
        Instant reviewedAt,
        SupportTicketEscalationDecision decision,
        UUID reassignedTo,
        String decisionNote
) {
}
