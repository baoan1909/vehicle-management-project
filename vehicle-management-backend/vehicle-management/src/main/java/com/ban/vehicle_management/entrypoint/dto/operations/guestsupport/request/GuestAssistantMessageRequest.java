package com.ban.vehicle_management.entrypoint.dto.operations.guestsupport.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record GuestAssistantMessageRequest(
        @NotBlank @Size(max = 1000) String message,
        @Size(max = 100) String sessionId
) {
}
