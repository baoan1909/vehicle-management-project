package com.ban.vehicle_management.application.parking.parkingsummary.model.result;

import com.ban.vehicle_management.domain.parking.parkinglevel.model.ParkingLevel;
import com.ban.vehicle_management.domain.parking.parkingspace.model.ParkingSpace;
import com.ban.vehicle_management.domain.parking.zone.model.Zone;
import java.util.List;

public record LevelMapResult(
        ParkingLevel level,
        List<Zone> zones,
        List<ParkingSpace> spaces,
        SpaceSummaryResult summary
) {
}
