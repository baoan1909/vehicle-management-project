package com.ban.vehicle_management.domain.hardware.device.model;

import com.ban.vehicle_management.domain.common.model.AuditableDomainModel;
import com.ban.vehicle_management.shared.enumeration.hardware.DeviceStatus;
import com.ban.vehicle_management.shared.enumeration.hardware.DeviceType;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Device extends AuditableDomainModel {

    private UUID deviceId;
    private UUID parkingLotId;
    private UUID laneId;
    private String deviceCode;
    private DeviceType deviceType;
    private String name;
    private String ipAddress;
    private DeviceStatus status;
    private Instant lastHeartbeatAt;
    private Map<String, Object> config;
}

