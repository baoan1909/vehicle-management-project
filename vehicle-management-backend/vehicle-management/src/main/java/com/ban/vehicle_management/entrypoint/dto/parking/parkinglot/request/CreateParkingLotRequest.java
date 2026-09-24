package com.ban.vehicle_management.entrypoint.dto.parking.parkinglot.request;

import java.util.UUID;

public record CreateParkingLotRequest(
        UUID organizationId,
        String code,
        String name,
        String address,
        Integer totalCapacity
) {
}
