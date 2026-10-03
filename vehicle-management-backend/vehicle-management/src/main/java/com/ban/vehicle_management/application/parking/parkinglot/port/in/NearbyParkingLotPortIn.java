package com.ban.vehicle_management.application.parking.parkinglot.port.in;

import com.ban.vehicle_management.application.parking.parkinglot.model.result.NearbyParkingLotResult;
import java.math.BigDecimal;
import java.util.List;

public interface NearbyParkingLotPortIn {
    List<NearbyParkingLotResult> findNearby(
            BigDecimal latitude,
            BigDecimal longitude,
            BigDecimal radiusKm,
            int limit
    );
}
