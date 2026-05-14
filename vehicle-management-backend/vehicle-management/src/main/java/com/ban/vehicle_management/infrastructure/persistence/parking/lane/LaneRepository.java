package com.ban.vehicle_management.infrastructure.persistence.parking.lane;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LaneRepository extends JpaRepository<LaneEntity, UUID> {
}
