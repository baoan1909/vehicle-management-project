package com.ban.vehicle_management.infrastructure.persistence.database.repository.hardware;

import com.ban.vehicle_management.infrastructure.persistence.database.entity.hardware.DeviceEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.dashboard.DeviceStatusCountProjection;
import java.util.List;

public interface DeviceRepository
        extends JpaRepository<DeviceEntity, UUID>,
        JpaSpecificationExecutor<DeviceEntity> {

    boolean existsByDeviceCode(String deviceCode);

    boolean existsByDeviceCodeAndDeviceIdNot(
            String deviceCode,
            UUID deviceId
    );

    @Query(value = """
            select device_type as "deviceType", status as "status", count(*) as "total"
            from hardware.devices
            where status <> 'RETIRED'
            group by device_type, status
            """, nativeQuery = true)
    List<DeviceStatusCountProjection> findDashboardStatusCounts();
}
