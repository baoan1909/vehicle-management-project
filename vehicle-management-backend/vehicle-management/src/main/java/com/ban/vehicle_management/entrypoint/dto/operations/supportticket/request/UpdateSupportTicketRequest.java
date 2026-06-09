package com.ban.vehicle_management.entrypoint.dto.operations.supportticket.request;

import java.util.UUID;

public record UpdateSupportTicketRequest(
        UUID categoryId,
        String title,
        String content
) {
}