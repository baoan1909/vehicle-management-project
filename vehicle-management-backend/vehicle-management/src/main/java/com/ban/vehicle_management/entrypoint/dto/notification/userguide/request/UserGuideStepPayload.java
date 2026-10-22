package com.ban.vehicle_management.entrypoint.dto.notification.userguide.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UserGuideStepPayload(
        @NotNull @Min(0) Integer displayOrder,
        @NotBlank @Size(max = 255) String title,
        @NotBlank @Size(max = 10000) String body,
        @Size(max = 500) String mediaObjectKey,
        @Size(max = 120) String anchorKey,
        @NotBlank String placement,
        @Min(0) @Max(600000) Integer collapseMs
) {
}
