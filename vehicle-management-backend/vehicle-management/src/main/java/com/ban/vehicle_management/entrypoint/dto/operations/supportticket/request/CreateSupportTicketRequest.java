package com.ban.vehicle_management.entrypoint.dto.operations.supportticket.request;

import java.util.UUID;

public record CreateSupportTicketRequest(
        UUID categoryId,
        String title,
        String content
) {
}