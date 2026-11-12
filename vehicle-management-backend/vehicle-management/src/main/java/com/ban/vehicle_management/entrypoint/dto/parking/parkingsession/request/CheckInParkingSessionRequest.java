package com.ban.vehicle_management.entrypoint.dto.parking.parkingsession.request;

import java.util.UUID;

public record CheckInParkingSessionRequest(
        String cardUid,
        UUID laneId,
        UUID vehicleTypeId,
        String licensePlate,
        String note,
        Boolean plateFormatConfirmed,
        Boolean plateIdentityOverride,
        UUID parkingSpaceId,
        UUID spaceHoldId,
        String subscriptionQrPayload,
        String reservationQrPayload
) {
    public CheckInParkingSessionRequest(
            String cardUid,
            UUID laneId,
            UUID vehicleTypeId,
            String licensePlate,
            String note
    ) {
        this(cardUid, laneId, vehicleTypeId, licensePlate, note, false, false, null, null, null, null);
    }

    public CheckInParkingSessionRequest(String cardUid, UUID laneId, UUID vehicleTypeId,
            String licensePlate, String note, Boolean plateFormatConfirmed,
            Boolean plateIdentityOverride, UUID parkingSpaceId, String subscriptionQrPayload) {
        this(cardUid, laneId, vehicleTypeId, licensePlate, note, plateFormatConfirmed,
                plateIdentityOverride, parkingSpaceId, null, subscriptionQrPayload, null);
    }
}
