package com.ban.vehicle_management.application.parking.parkinglot.port.in;

import com.ban.vehicle_management.application.parking.parkinglot.model.result.PublicParkingLotDirectoryResult;
import java.util.List;

public interface PublicParkingLotDirectoryPortIn {
    List<PublicParkingLotDirectoryResult> getDirectory();
}
