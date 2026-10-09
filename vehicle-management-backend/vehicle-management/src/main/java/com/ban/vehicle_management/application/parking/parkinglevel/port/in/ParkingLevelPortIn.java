package com.ban.vehicle_management.application.parking.parkinglevel.port.in;

import com.ban.vehicle_management.domain.parking.parkinglevel.model.ParkingLevel;
import com.ban.vehicle_management.shared.enumeration.parking.ParkingLevelStatus;
import java.util.List;
import java.util.UUID;

public interface ParkingLevelPortIn {

    ParkingLevel createParkingLevel(ParkingLevel parkingLevel);

    ParkingLevel getParkingLevelById(UUID parkingLevelId);

    List<ParkingLevel> getParkingLevels(UUID parkingLotId, ParkingLevelStatus status, String keyword);

    ParkingLevel updateParkingLevel(UUID parkingLevelId, ParkingLevel parkingLevel);

    void deleteParkingLevel(UUID parkingLevelId);

    ParkingLevel activateParkingLevel(UUID parkingLevelId);

    ParkingLevel markParkingLevelMaintenance(UUID parkingLevelId);

    ParkingLevel closeParkingLevel(UUID parkingLevelId);
}