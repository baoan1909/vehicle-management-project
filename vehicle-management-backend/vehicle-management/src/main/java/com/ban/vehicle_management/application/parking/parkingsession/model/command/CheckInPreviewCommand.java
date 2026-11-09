package com.ban.vehicle_management.application.parking.parkingsession.model.command;

import java.util.UUID;

public record CheckInPreviewCommand(
        String cardUid,
        UUID laneId,
        UUID vehicleTypeId,
        String licensePlate,
        String subscriptionQrPayload,
        String reservationQrPayload
) {
    public CheckInPreviewCommand(String cardUid, UUID laneId, UUID vehicleTypeId, String licensePlate) {
        this(cardUid, laneId, vehicleTypeId, licensePlate, null, null);
    }

    public CheckInPreviewCommand(String cardUid, UUID laneId, UUID vehicleTypeId,
            String licensePlate, String subscriptionQrPayload) {
        this(cardUid, laneId, vehicleTypeId, licensePlate, subscriptionQrPayload, null);
    }
}

