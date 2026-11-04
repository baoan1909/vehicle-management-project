package com.ban.vehicle_management.entrypoint.dto.catalog.availability.request;

import jakarta.validation.constraints.NotEmpty;
import java.util.Set;
import java.util.UUID;

public record UpdateParkingLotVehicleServiceRequest(
        @NotEmpty Set<UUID> parkingLotIds,
        boolean enabled
) {
}
