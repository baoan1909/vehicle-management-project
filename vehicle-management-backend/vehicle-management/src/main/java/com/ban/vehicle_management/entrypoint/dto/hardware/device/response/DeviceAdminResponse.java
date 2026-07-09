package com.ban.vehicle_management.entrypoint.dto.hardware.device.response;

import com.ban.vehicle_management.shared.enumeration.hardware.DeviceStatus;
import com.ban.vehicle_management.shared.enumeration.hardware.DeviceType;
import java.util.Map;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class DeviceAdminResponse {

    private UUID deviceId;
    private UUID parkingLotId;
    private UUID laneId;
    private String deviceCode;
    private DeviceType deviceType;
    private String name;
    private String ipAddress;
    private DeviceStatus status;
    private Map<String, Object> config;
    private String createdAt;
    private UUID createdBy;
    private String updatedAt;
    private UUID updatedBy;
}