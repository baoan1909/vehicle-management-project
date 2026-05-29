package com.ban.vehicle_management.entrypoint.dto.parking.zone.request;

import java.util.UUID;

public record CreateZoneRequest(
        UUID parkingLotId,
        String code,
        String name,
        UUID vehicleTypeId,
        Integer capacity
) {
}