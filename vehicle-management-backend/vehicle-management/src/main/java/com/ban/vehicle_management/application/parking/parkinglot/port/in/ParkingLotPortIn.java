package com.ban.vehicle_management.application.parking.parkinglot.port.in;

import com.ban.vehicle_management.domain.parking.parkinglot.model.ParkingLot;
import com.ban.vehicle_management.shared.enumeration.parking.ParkingLotStatus;
import java.util.List;
import java.util.UUID;

public interface ParkingLotPortIn {
    ParkingLot createParkingLot(ParkingLot parkingLot);

    ParkingLot getParkingLotById(UUID parkingLotId);

    List<ParkingLot> getParkingLots(ParkingLotStatus status, String keyword);

    ParkingLot updateParkingLot(UUID parkingLotId, ParkingLot parkingLot);

    void deleteParkingLot(UUID parkingLotId);

    ParkingLot activateParkingLot(UUID parkingLotId);

    ParkingLot markParkingLotMaintenance(UUID parkingLotId);

    ParkingLot closeParkingLot(UUID parkingLotId);
}