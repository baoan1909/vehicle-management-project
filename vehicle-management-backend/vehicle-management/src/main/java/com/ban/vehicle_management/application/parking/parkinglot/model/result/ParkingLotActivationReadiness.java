package com.ban.vehicle_management.application.parking.parkinglot.model.result;

public record ParkingLotActivationReadiness(
        boolean catalogAndPricingReady,
        boolean activeZoneReady,
        boolean activeGateReady,
        boolean activeInLaneReady,
        boolean activeOutLaneReady,
        boolean publishedSpaceLayoutsReady
) {
    public boolean ready() {
        return catalogAndPricingReady
                && activeZoneReady
                && activeGateReady
                && activeInLaneReady
                && activeOutLaneReady
                && publishedSpaceLayoutsReady;
    }
}
