package com.ban.vehicle_management.application.parking.parkingsummary.model.result;

import java.util.UUID;

public record SpaceReconciliationIssue(
        String issueType,
        UUID parkingSpaceId,
        UUID parkingSessionId,
        UUID subscriptionId,
        String detail
) {
}
