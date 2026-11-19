package com.ban.vehicle_management.entrypoint.dto.operations.supportticket.request;

import java.util.UUID;

public record CreateSupportTicketRequest(
        UUID categoryId,
        String title,
        String content,
        UUID organizationId,
        UUID parkingLotId,
        String contextKey
) {
    public CreateSupportTicketRequest(UUID categoryId, String title, String content) {
        this(categoryId, title, content, null, null, null);
    }
}
