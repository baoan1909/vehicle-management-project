package com.ban.vehicle_management.application.parking.location.port.in;

import com.ban.vehicle_management.application.parking.location.model.ParkingLocationFeatureStatus;

public interface ParkingLocationFeaturePortIn {

    ParkingLocationFeatureStatus getPublicStatus();

    void requireAdminAddressV2();

    void requirePublicNearbySearch();

    void requireCustomerParkingMap();

    void requireGeocodingProvider();
}