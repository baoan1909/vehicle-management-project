package com.ban.vehicle_management.entrypoint.dto.operations.approvalrequest.request;

import com.ban.vehicle_management.shared.enumeration.operations.SupportTicketEscalationReason;

public record CreateSupportTicketEscalationRequest(
        SupportTicketEscalationReason reasonCode,
        String description
) {
}
