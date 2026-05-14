package com.ban.vehicle_management.infrastructure.persistence.parking.parkingevent;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ParkingEventRepository extends JpaRepository<ParkingEventEntity, UUID> {
}
