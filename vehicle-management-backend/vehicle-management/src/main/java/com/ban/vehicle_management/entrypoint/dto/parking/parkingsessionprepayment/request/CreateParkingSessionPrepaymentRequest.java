package com.ban.vehicle_management.entrypoint.dto.parking.parkingsessionprepayment.request;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateParkingSessionPrepaymentRequest(
        @NotBlank String licensePlate,
        @NotBlank String cardNumber,
        @NotNull @Min(5) @Max(240) Integer estimatedMinutes,
        @AssertTrue boolean policyAccepted
) {
}
