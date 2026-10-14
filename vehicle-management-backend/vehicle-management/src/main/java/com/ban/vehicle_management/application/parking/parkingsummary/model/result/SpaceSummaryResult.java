package com.ban.vehicle_management.application.parking.parkingsummary.model.result;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record SpaceSummaryResult(
        UUID scopeId,
        String scopeType,
        long total,
        long available,
        long occupied,
        long occupiedVisitor,
        long occupiedRegistered,
        long occupiedUnknown,
        long reserved,
        long maintenance,
        long subscriptionAllocated,
        List<SpaceSummaryResult> zones,
        Instant calculatedAt
) {
}
