package com.ban.vehicle_management.entrypoint.dto.operations.guestsupport.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record GuestAssistantMessageRequest(
        @NotBlank @Size(max = 1000) String message,
        @Size(max = 100) String sessionId,
        @Size(max = 512) String captchaProof,
        @Size(max = 500) String contextQuestion
) {
    public GuestAssistantMessageRequest(String message, String sessionId, String captchaProof) {
        this(message, sessionId, captchaProof, null);
    }
}
