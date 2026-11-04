package com.ban.vehicle_management.application.parking.parkinglotreview.model;

import java.time.Instant;
import java.util.UUID;

public record ReviewListItem(
        UUID reviewId, UUID parkingLotId, String parkingLotName, UUID reviewerCustomerId,
        String reviewerDisplayName,
        int rating, String comment, Instant createdAt, Instant updatedAt
) {
    public boolean edited() {
        return updatedAt != null && createdAt != null && updatedAt.isAfter(createdAt);
    }
}
