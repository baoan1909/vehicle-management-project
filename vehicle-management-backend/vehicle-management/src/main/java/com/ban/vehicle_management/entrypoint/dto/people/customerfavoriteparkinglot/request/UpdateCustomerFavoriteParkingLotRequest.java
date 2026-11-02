package com.ban.vehicle_management.entrypoint.dto.people.customerfavoriteparkinglot.request;

import jakarta.validation.constraints.NotNull;

public record UpdateCustomerFavoriteParkingLotRequest(
        @NotNull Boolean favorite
) {
}

