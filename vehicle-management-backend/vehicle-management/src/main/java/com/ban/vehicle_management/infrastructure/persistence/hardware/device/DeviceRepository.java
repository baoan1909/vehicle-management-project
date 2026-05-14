package com.ban.vehicle_management.infrastructure.persistence.hardware.device;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeviceRepository extends JpaRepository<DeviceEntity, UUID> {
}
