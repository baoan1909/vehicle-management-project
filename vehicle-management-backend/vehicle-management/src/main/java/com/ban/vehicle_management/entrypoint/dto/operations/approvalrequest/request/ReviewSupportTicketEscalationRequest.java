package com.ban.vehicle_management.entrypoint.dto.operations.approvalrequest.request;

import com.ban.vehicle_management.shared.enumeration.operations.SupportTicketEscalationDecision;
import java.util.UUID;

public record ReviewSupportTicketEscalationRequest(
        SupportTicketEscalationDecision decision,
        UUID assignedTo,
        String note
) {
}
