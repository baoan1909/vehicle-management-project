package com.ban.vehicle_management.infrastructure.persistence.database.repository.parking;

import com.ban.vehicle_management.infrastructure.persistence.database.entity.parking.ParkingEventEntity;
import com.ban.vehicle_management.shared.enumeration.parking.ParkingEventType;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ParkingEventRepository extends JpaRepository<ParkingEventEntity, UUID> {

    Optional<ParkingEventEntity> findFirstByParkingSessionIdAndEventTypeOrderByEventTimeDesc(
            UUID parkingSessionId,
            ParkingEventType eventType
    );
}


