package com.ban.vehicle_management.application.parking.parkinglot.port.in;

import com.ban.vehicle_management.application.parking.parkinglot.model.result.PublicParkingLotDirectoryResult;
import com.ban.vehicle_management.domain.parking.parkinglot.model.ParkingLot;
import java.util.List;

public interface PublicParkingLotDirectoryPortIn {
    List<ParkingLot> getActiveParkingLots();

    List<PublicParkingLotDirectoryResult> getDirectory();
}
