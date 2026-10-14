package com.ban.vehicle_management.entrypoint.dto.parking.parkingspace.request;

import java.util.List;
import java.util.UUID;

public record BulkCreateParkingSpaceRequest(
        UUID zoneId,
        List<CreateParkingSpaceRequest> spaces
) {
}
