package com.ban.vehicle_management.entrypoint.dto.accesscontrol.subscription.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record VerifySubscriptionQrRequest(
        @NotBlank
        @Size(max = 128)
        String qrPayload,
        @NotNull
        UUID parkingLotId,
        @Size(max = 30)
        String licensePlate
) {
}
