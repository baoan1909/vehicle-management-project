package com.ban.vehicle_management.application.parking.location.port.out;

public interface ParkingLocationFeaturePortOut {

    boolean isAdminAddressV2Enabled();

    boolean isPublicNearbySearchEnabled();

    boolean isCustomerParkingMapEnabled();

    boolean isGeocodingProviderEnabled();
}