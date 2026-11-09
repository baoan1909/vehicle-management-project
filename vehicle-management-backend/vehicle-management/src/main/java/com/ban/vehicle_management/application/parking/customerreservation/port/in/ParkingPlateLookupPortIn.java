package com.ban.vehicle_management.application.parking.customerreservation.port.in;

import com.ban.vehicle_management.application.parking.customerreservation.model.ParkingPlateLookupResult;

public interface ParkingPlateLookupPortIn {
    ParkingPlateLookupResult lookup(String licensePlate);
}
