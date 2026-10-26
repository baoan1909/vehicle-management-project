package com.ban.vehicle_management.entrypoint.dto.operations.guestsupport.request;

import com.ban.vehicle_management.application.operations.guestsupport.model.CaptchaPurpose;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record VerifyCaptchaRequest(
        @NotNull CaptchaPurpose purpose,
        @NotBlank @Size(max = 4096) String challengeToken,
        @Size(max = 100) String sessionId
) {
}
