package com.ban.vehicle_management.infrastructure.persistence.database.repository.hardware;

import com.ban.vehicle_management.infrastructure.persistence.database.entity.hardware.DeviceEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface DeviceRepository
        extends JpaRepository<DeviceEntity, UUID>,
        JpaSpecificationExecutor<DeviceEntity> {

    boolean existsByDeviceCode(String deviceCode);

    boolean existsByDeviceCodeAndDeviceIdNot(
            String deviceCode,
            UUID deviceId
    );
}