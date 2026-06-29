package com.ban.vehicle_management.entrypoint.dto.parking.parkingsession.request;

import java.util.UUID;

public record CheckOutParkingSessionRequest(
        UUID laneId,
        String cardUid,
        String licensePlate,
        String note
) {
}
