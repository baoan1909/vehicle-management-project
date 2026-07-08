package com.ban.vehicle_management.entrypoint.dto.operations.chatmessage.request;

import java.util.UUID;

public record MarkConversationReadRequest(UUID messageId) {
}
