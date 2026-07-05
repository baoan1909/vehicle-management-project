package com.ban.vehicle_management.entrypoint.dto.operations.supportticket.request;

import java.util.UUID;

public record AssignSupportTicketRequest(
        UUID assignedTo
) {
}