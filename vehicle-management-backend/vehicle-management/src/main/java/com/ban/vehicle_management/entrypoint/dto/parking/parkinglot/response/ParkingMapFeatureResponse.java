package com.ban.vehicle_management.entrypoint.dto.parking.parkinglot.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ParkingMapFeatureResponse {

    private boolean customerParkingMapEnabled;
    private boolean publicNearbySearchEnabled;
    private boolean geocodingProviderEnabled;
}