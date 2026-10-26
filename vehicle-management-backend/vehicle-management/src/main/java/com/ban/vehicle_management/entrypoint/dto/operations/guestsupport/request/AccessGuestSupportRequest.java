package com.ban.vehicle_management.entrypoint.dto.operations.guestsupport.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AccessGuestSupportRequest(
        @NotBlank @Size(max = 32) String referenceCode,
        @NotBlank @Size(max = 128) String capabilityToken
) {
}
