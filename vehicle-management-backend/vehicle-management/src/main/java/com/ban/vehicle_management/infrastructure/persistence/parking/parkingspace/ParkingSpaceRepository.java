package com.ban.vehicle_management.infrastructure.persistence.parking.parkingspace;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ParkingSpaceRepository extends JpaRepository<ParkingSpaceEntity, UUID> {
}
