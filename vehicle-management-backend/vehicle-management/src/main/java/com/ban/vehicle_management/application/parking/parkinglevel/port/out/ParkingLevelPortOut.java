package com.ban.vehicle_management.application.parking.parkinglevel.port.out;

import com.ban.vehicle_management.domain.parking.parkinglevel.model.ParkingLevel;
import com.ban.vehicle_management.shared.enumeration.parking.ParkingLevelStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ParkingLevelPortOut {

    ParkingLevel save(ParkingLevel parkingLevel);

    Optional<ParkingLevel> findById(UUID parkingLevelId);

    List<ParkingLevel> findAll(UUID parkingLotId, ParkingLevelStatus status, String keyword);

    boolean existsByParkingLotIdAndCode(UUID parkingLotId, String code);

    boolean existsByParkingLotIdAndCodeAndParkingLevelIdNot(UUID parkingLotId, String code, UUID parkingLevelId);

    boolean existsConfigurableParkingLotById(UUID parkingLotId);

    long countByParkingLotId(UUID parkingLotId);

    long countZonesByParkingLevelId(UUID parkingLevelId);

    void delete(UUID parkingLevelId);
}