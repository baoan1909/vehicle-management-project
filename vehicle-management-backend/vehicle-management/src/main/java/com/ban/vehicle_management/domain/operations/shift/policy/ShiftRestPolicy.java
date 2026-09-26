package com.ban.vehicle_management.domain.operations.shift.policy;

import java.time.Duration;
import java.time.Instant;

public final class ShiftRestPolicy {

    private final Duration minimumRest;

    public ShiftRestPolicy(Duration minimumRest) {
        if (minimumRest == null || minimumRest.isZero() || minimumRest.isNegative()) {
            throw new IllegalArgumentException("minimumRest must be greater than zero");
        }
        this.minimumRest = minimumRest;
    }

    public boolean isSatisfied(Instant previousEnd, Instant nextStart) {
        if (previousEnd == null || nextStart == null || nextStart.isBefore(previousEnd)) {
            return false;
        }
        return Duration.between(previousEnd, nextStart).compareTo(minimumRest) >= 0;
    }

    public Duration minimumRest() {
        return minimumRest;
    }
}
