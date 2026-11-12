package com.ban.vehicle_management.entrypoint.dto.parking.checkinspacehold.request;

import java.util.UUID;

public record CreateCheckInSpaceHoldRequest(
        UUID requestId,
        String cardUid,
        UUID laneId,
        UUID vehicleTypeId,
        String licensePlate,
        String subscriptionQrPayload,
        String reservationQrPayload
) {
}
