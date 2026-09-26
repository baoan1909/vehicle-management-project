package com.ban.vehicle_management.entrypoint.dto.parking.zone.request;

import java.util.UUID;
import java.util.Set;

public record CreateZoneRequest(
        UUID parkingLotId,
        String code,
        String name,
        UUID vehicleTypeId,
        Set<UUID> vehicleTypeIds,
        Integer capacity
) {
}
