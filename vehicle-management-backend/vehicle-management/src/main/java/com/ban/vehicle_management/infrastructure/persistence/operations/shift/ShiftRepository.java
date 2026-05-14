package com.ban.vehicle_management.infrastructure.persistence.operations.shift;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ShiftRepository extends JpaRepository<ShiftEntity, UUID> {
}
