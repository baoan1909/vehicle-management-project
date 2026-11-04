package com.ban.vehicle_management.entrypoint.dto.catalog.vehicletype.response;

public record VehicleTypeDeactivationImpactResponse(
        long activePriceRuleCount,
        long activeCustomerVehicleCount,
        long activeSubscriptionCount,
        long openParkingSessionCount,
        long assignedZoneCount,
        boolean requiresPriceRuleCascade,
        boolean hasOperationalBlockers
) {
}
