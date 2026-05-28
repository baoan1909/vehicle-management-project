package com.ban.vehicle_management.application.parking.parkinglot.port.out;

import com.ban.vehicle_management.domain.parking.parkinglot.model.ParkingLot;
import com.ban.vehicle_management.shared.enumeration.parking.ParkingLotStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ParkingLotPortOut {
    ParkingLot save(ParkingLot parkingLot);

    Optional<ParkingLot> findById(UUID parkingLotId);

    List<ParkingLot> findAll(ParkingLotStatus status, String keyword);

    boolean existsByCode(String code);

    boolean existsByCodeAndParkingLotIdNot(String code, UUID parkingLotId);
}