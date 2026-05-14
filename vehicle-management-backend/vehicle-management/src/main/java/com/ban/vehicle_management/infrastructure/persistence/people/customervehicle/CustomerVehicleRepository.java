package com.ban.vehicle_management.infrastructure.persistence.people.customervehicle;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerVehicleRepository extends JpaRepository<CustomerVehicleEntity, UUID> {
}
