package com.ban.vehicle_management.entrypoint.dto.operations.chatconversation.request;

import java.util.UUID;

public record CreateInternalDirectConversationRequest(UUID targetAccountId) {
}
