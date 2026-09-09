package com.ban.vehicle_management.application.operations.approvalrequest.model.command;

import com.ban.vehicle_management.shared.enumeration.operations.SupportTicketEscalationReason;

public record CreateSupportTicketEscalationCommand(
        SupportTicketEscalationReason reasonCode,
        String description,
        String idempotencyKey
) {
}
