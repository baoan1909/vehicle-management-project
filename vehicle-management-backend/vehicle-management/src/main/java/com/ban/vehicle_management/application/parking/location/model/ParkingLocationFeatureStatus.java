package com.ban.vehicle_management.application.parking.location.model;

public record ParkingLocationFeatureStatus(
        boolean customerParkingMapEnabled,
        boolean publicNearbySearchEnabled,
        boolean geocodingProviderEnabled
) {
}