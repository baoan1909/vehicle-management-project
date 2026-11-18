package com.ban.vehicle_management.application.operations.supportticket.mapper;

import com.ban.vehicle_management.application.operations.supportticket.model.SupportTicketSnapshotPayload;
import com.ban.vehicle_management.domain.operations.supportticket.model.SupportTicket;
import com.ban.vehicle_management.shared.enumeration.operations.SupportTicketEventType;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class SupportTicketSnapshotJsonMapper {

    private final ObjectMapper objectMapper;

    public SupportTicketSnapshotJsonMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String toJson(
            UUID eventId,
            SupportTicketEventType eventType,
            SupportTicket ticket,
            UUID actorAccountId,
            Instant occurredAt
    ) {
        SupportTicketSnapshotPayload payload = new SupportTicketSnapshotPayload(
                1,
                eventId,
                eventType,
                ticket.getSupportTicketId(),
                "TK-" + ticket.getSupportTicketId().toString().substring(0, 8).toUpperCase(),
                ticket.getTitle(),
                ticket.getContent(),
                ticket.getCategoryCode(),
                ticket.getCategoryName(),
                ticket.getPriority(),
                ticket.getStatus(),
                ticket.getAssignedTo(),
                actorAccountId,
                occurredAt,
                false
        );
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Support ticket snapshot could not be serialized", exception);
        }
    }
}
