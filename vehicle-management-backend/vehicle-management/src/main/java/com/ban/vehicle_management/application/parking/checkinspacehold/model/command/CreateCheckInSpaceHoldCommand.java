package com.ban.vehicle_management.application.parking.checkinspacehold.model.command;

import java.util.UUID;

public record CreateCheckInSpaceHoldCommand(
        UUID requestId,
        String cardUid,
        UUID laneId,
        UUID vehicleTypeId,
        String licensePlate,
        String subscriptionQrPayload,
        String reservationQrPayload
) {
}
