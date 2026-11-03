package com.ban.vehicle_management.entrypoint.dto.parking.parkinglotreview.response;

import java.time.Instant;
import java.util.UUID;

public record MyParkingLotReviewResponse(
        boolean eligible,
        String reasonCode,
        UUID parkingLotId,
        String parkingLotName,
        CurrentReview currentReview
) {
    public record CurrentReview(int rating, String comment, Instant createdAt,
                                Instant updatedAt, boolean edited) {
    }
}
