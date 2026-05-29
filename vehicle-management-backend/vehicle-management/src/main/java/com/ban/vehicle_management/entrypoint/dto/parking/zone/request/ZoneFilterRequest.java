package com.ban.vehicle_management.entrypoint.dto.parking.zone.request;

import com.ban.vehicle_management.shared.enumeration.parking.ZoneStatus;
import java.util.UUID;

public record ZoneFilterRequest(
        UUID parkingLotId,
        UUID vehicleTypeId,
        ZoneStatus status,
        String keyword
) {
}