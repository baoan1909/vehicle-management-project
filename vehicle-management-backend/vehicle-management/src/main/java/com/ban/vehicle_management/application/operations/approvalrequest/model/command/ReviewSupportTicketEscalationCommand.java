package com.ban.vehicle_management.application.operations.approvalrequest.model.command;

import com.ban.vehicle_management.shared.enumeration.operations.SupportTicketEscalationDecision;
import java.util.UUID;

public record ReviewSupportTicketEscalationCommand(
        SupportTicketEscalationDecision decision,
        UUID assignedTo,
        String note
) {
}
