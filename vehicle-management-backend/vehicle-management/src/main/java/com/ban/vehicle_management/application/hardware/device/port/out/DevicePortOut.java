package com.ban.vehicle_management.application.hardware.device.port.out;

import com.ban.vehicle_management.domain.hardware.device.model.Device;
import com.ban.vehicle_management.shared.enumeration.hardware.DeviceStatus;
import com.ban.vehicle_management.shared.enumeration.hardware.DeviceType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DevicePortOut {

    Device save(Device device);

    Optional<Device> findById(UUID deviceId);

    List<Device> findAll(
            UUID parkingLotId,
            UUID laneId,
            DeviceType deviceType,
            DeviceStatus status,
            String keyword
    );

    boolean existsByDeviceCode(String deviceCode);

    boolean existsByDeviceCodeAndDeviceIdNot(
            String deviceCode,
            UUID deviceId
    );

    boolean existsLaneInParkingLot(UUID laneId, UUID parkingLotId);

    boolean existsNonClosedLaneById(UUID laneId);
}