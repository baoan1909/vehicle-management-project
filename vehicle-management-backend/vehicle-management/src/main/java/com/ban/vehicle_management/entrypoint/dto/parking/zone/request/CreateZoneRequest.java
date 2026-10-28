package com.ban.vehicle_management.entrypoint.dto.parking.zone.request;

import com.ban.vehicle_management.shared.enumeration.parking.TrackingMode;
import java.util.UUID;
import java.util.Set;

public record CreateZoneRequest(
        UUID parkingLotId,
        UUID parkingLevelId,
        String name,
        UUID vehicleTypeId,
        Set<UUID> vehicleTypeIds,
        Integer capacity,
        TrackingMode trackingMode
) {
}
