package com.ban.vehicle_management.infrastructure.persistence.parking.zone;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ZoneRepository extends JpaRepository<ZoneEntity, UUID> {
}
