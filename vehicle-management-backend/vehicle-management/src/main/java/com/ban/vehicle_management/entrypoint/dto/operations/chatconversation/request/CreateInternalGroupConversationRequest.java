package com.ban.vehicle_management.entrypoint.dto.operations.chatconversation.request;

import java.util.Set;
import java.util.UUID;

public record CreateInternalGroupConversationRequest(String title, Set<UUID> memberAccountIds) {
}
