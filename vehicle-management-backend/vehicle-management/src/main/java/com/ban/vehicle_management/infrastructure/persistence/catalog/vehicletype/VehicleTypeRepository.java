package com.ban.vehicle_management.infrastructure.persistence.catalog.vehicletype;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VehicleTypeRepository extends JpaRepository<VehicleTypeEntity, UUID> {
}
