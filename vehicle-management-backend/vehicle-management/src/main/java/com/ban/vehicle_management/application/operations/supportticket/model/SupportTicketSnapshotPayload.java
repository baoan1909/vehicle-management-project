package com.ban.vehicle_management.application.operations.supportticket.model;

import com.ban.vehicle_management.shared.enumeration.operations.SupportTicketCategoryPriority;
import com.ban.vehicle_management.shared.enumeration.operations.SupportTicketEventType;
import com.ban.vehicle_management.shared.enumeration.operations.SupportTicketStatus;
import java.time.Instant;
import java.util.UUID;

public record SupportTicketSnapshotPayload(
        int schemaVersion,
        UUID eventId,
        SupportTicketEventType eventType,
        UUID ticketId,
        String ticketCode,
        String title,
        String content,
        String categoryCode,
        String categoryName,
        SupportTicketCategoryPriority priority,
        SupportTicketStatus snapshotStatus,
        UUID assignedTo,
        UUID actorAccountId,
        Instant occurredAt,
        boolean legacyInferred
) {
}
