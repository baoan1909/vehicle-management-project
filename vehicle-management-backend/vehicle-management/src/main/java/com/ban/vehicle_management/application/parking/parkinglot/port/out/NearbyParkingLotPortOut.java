package com.ban.vehicle_management.application.parking.parkinglot.port.out;

import com.ban.vehicle_management.application.parking.parkinglot.model.result.NearbyParkingLotResult;
import java.math.BigDecimal;
import java.util.List;

public interface NearbyParkingLotPortOut {
    List<NearbyParkingLotResult> findNearby(
            BigDecimal latitude,
            BigDecimal longitude,
            BigDecimal radiusMeters,
            int limit
    );
}
