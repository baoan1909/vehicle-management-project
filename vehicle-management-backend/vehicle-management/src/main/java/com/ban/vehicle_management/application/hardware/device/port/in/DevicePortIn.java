package com.ban.vehicle_management.application.hardware.device.port.in;

import com.ban.vehicle_management.domain.hardware.device.model.Device;
import com.ban.vehicle_management.shared.enumeration.hardware.DeviceStatus;
import com.ban.vehicle_management.shared.enumeration.hardware.DeviceType;
import java.util.List;
import java.util.UUID;

public interface DevicePortIn {

    Device createDevice(Device device);

    Device getDeviceById(UUID deviceId);

    List<Device> getDevices(
            UUID parkingLotId,
            UUID laneId,
            DeviceType deviceType,
            DeviceStatus status,
            String keyword
    );

    Device updateDevice(UUID deviceId, Device request);

    Device activateDevice(UUID deviceId);

    Device markDeviceOffline(UUID deviceId);

    Device markDeviceMaintenance(UUID deviceId);

    void deleteDevice(UUID deviceId);
}