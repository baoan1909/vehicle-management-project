package com.ban.vehicle_management.infrastructure.persistence.parking.parkinglot;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ParkingLotRepository extends JpaRepository<ParkingLotEntity, UUID> {
}
