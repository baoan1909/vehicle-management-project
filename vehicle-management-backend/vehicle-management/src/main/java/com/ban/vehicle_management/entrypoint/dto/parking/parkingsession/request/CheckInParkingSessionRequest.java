package com.ban.vehicle_management.entrypoint.dto.parking.parkingsession.request;

import java.util.UUID;

public record CheckInParkingSessionRequest(
        String cardUid,
        UUID laneId,
        UUID vehicleTypeId,
        String licensePlate,
        String note
) {
}
