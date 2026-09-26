package com.ban.vehicle_management.entrypoint.dto.parking.parkingsession.request;

import java.util.UUID;

public record CheckInParkingSessionRequest(
        String cardUid,
        UUID laneId,
        UUID vehicleTypeId,
        String licensePlate,
        String note,
        Boolean plateFormatConfirmed,
        Boolean plateIdentityOverride
) {
    public CheckInParkingSessionRequest(
            String cardUid,
            UUID laneId,
            UUID vehicleTypeId,
            String licensePlate,
            String note
    ) {
        this(cardUid, laneId, vehicleTypeId, licensePlate, note, false, false);
    }
}
