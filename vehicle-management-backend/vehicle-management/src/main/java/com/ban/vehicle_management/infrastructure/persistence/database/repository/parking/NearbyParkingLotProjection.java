package com.ban.vehicle_management.infrastructure.persistence.database.repository.parking;

import com.ban.vehicle_management.shared.enumeration.parking.ParkingLotStatus;
import java.math.BigDecimal;
import java.util.UUID;

public interface NearbyParkingLotProjection {
    UUID getParkingLotId();

    String getName();

    String getAddressDisplay();

    BigDecimal getLatitude();

    BigDecimal getLongitude();

    ParkingLotStatus getStatus();

    double getDistanceMeters();
}
