package com.ban.vehicle_management.application.parking.parkinglot.port.out;

import com.ban.vehicle_management.domain.parking.parkinglot.model.ParkingLot;
import com.ban.vehicle_management.shared.enumeration.parking.ParkingLotStatus;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface ParkingLotPortOut {
    ParkingLot save(ParkingLot parkingLot);

    Optional<ParkingLot> findById(UUID parkingLotId);

    List<ParkingLot> findAll(
            ParkingLotStatus status,
            String keyword,
            Set<UUID> organizationIds,
            Set<UUID> parkingLotIds
    );

    boolean existsByOrganizationIdAndCode(UUID organizationId, String code);

    boolean existsByOrganizationIdAndCodeAndParkingLotIdNot(
            UUID organizationId,
            String code,
            UUID parkingLotId
    );

    boolean hasActiveZones(UUID parkingLotId);
}
