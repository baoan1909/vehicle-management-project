package com.ban.vehicle_management.application.parking.location.port.out;

import com.ban.vehicle_management.application.parking.location.model.ParkingLocationSearchResult;
import java.math.BigDecimal;
import java.util.List;

public interface ParkingLocationPortOut {
    List<ParkingLocationSearchResult> search(String query);

    List<ParkingLocationSearchResult> reverse(BigDecimal latitude, BigDecimal longitude);
}
