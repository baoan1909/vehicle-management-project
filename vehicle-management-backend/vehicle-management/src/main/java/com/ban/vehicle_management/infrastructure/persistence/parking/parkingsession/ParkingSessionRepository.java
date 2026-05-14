package com.ban.vehicle_management.infrastructure.persistence.parking.parkingsession;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ParkingSessionRepository extends JpaRepository<ParkingSessionEntity, UUID> {
}
