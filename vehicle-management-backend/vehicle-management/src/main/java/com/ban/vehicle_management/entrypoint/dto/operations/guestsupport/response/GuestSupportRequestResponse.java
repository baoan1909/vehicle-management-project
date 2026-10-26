package com.ban.vehicle_management.entrypoint.dto.operations.guestsupport.response;

import com.ban.vehicle_management.shared.enumeration.operations.SupportTicketStatus;
import java.time.Instant;
import java.util.UUID;

public record GuestSupportRequestResponse(
        String referenceCode,
        UUID categoryId,
        String title,
        String content,
        String maskedContact,
        SupportTicketStatus status,
        Instant createdAt,
        Instant capabilityExpiresAt,
        String capabilityToken
) {
}
