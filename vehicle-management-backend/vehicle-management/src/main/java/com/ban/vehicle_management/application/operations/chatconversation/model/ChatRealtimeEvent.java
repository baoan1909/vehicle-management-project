package com.ban.vehicle_management.application.operations.chatconversation.model;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

public record ChatRealtimeEvent(
        UUID conversationId,
        UUID messageId,
        Instant occurredAt,
        ChatRealtimeMessage message
) implements Serializable {

    private static final long serialVersionUID = 1L;
}
