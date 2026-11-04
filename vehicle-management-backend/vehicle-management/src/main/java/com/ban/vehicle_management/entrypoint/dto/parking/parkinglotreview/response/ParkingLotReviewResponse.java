package com.ban.vehicle_management.entrypoint.dto.parking.parkinglotreview.response;

import java.time.Instant;
import java.util.UUID;

public record ParkingLotReviewResponse(
        String parkingLotName,
        String reviewerDisplayName,
        UUID reviewerCustomerId,
        int rating,
        String comment,
        boolean verified,
        Instant createdAt,
        Instant updatedAt,
        boolean edited
) {
}
