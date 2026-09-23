package com.ban.vehicle_management.entrypoint.dto.ai.assistant.response;

import java.util.UUID;

public record AssistantMessageStatusResponse(UUID inputMessageId, String status, String errorCode, boolean terminal) {
}
