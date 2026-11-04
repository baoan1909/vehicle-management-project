package com.ban.vehicle_management.application.catalog.vehicletype.model;

public record VehicleTypeDeactivationImpact(
        long activePriceRuleCount,
        long activeCustomerVehicleCount,
        long activeSubscriptionCount,
        long openParkingSessionCount,
        long assignedZoneCount
) {
    public boolean requiresPriceRuleCascade() {
        return activePriceRuleCount > 0;
    }

    public boolean hasOperationalBlockers() {
        return activeSubscriptionCount > 0 || openParkingSessionCount > 0;
    }
}
