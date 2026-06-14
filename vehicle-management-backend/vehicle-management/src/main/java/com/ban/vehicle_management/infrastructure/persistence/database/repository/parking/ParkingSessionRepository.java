package com.ban.vehicle_management.infrastructure.persistence.database.repository.parking;

import com.ban.vehicle_management.infrastructure.persistence.database.entity.parking.ParkingSessionEntity;
import com.ban.vehicle_management.shared.enumeration.parking.ParkingSessionStatus;
import java.util.Collection;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ParkingSessionRepository extends JpaRepository<ParkingSessionEntity, UUID> {

    boolean existsByCardId(UUID cardId);

    boolean existsByCardIdAndStatusIn(UUID cardId, Collection<ParkingSessionStatus> statuses);

    boolean existsByVehicleTypeIdAndStatusIn(UUID vehicleTypeId, Collection<ParkingSessionStatus> statuses);

    long countByZoneIdAndStatus(UUID zoneId, ParkingSessionStatus status);

    boolean existsByZoneIdAndStatus(UUID zoneId, ParkingSessionStatus status);
}


