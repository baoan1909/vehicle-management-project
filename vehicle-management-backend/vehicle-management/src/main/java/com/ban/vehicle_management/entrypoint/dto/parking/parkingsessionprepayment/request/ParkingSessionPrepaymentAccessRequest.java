package com.ban.vehicle_management.entrypoint.dto.parking.parkingsessionprepayment.request;

import jakarta.validation.constraints.NotBlank;

public record ParkingSessionPrepaymentAccessRequest(
        @NotBlank String licensePlate,
        @NotBlank String cardNumber
) {
}
