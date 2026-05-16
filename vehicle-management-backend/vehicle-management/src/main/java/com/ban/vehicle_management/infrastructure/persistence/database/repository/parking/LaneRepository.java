package com.ban.vehicle_management.infrastructure.persistence.database.repository.parking;

import com.ban.vehicle_management.infrastructure.persistence.database.entity.parking.LaneEntity;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LaneRepository extends JpaRepository<LaneEntity, UUID> {
}


