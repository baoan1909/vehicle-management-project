package com.ban.vehicle_management.application.parking.parkinglotreview.model;

import java.time.Instant;
import java.util.UUID;

public record ReviewEligibilityEvidence(UUID parkingSessionId, UUID paymentId, Instant occurredAt) {
    public static ReviewEligibilityEvidence session(UUID id, Instant occurredAt) {
        return new ReviewEligibilityEvidence(id, null, occurredAt);
    }

    public static ReviewEligibilityEvidence payment(UUID id, Instant occurredAt) {
        return new ReviewEligibilityEvidence(null, id, occurredAt);
    }
}
