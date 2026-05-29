package com.ban.vehicle_management.entrypoint.dto.parking.zone.response;

import com.ban.vehicle_management.shared.enumeration.parking.ZoneStatus;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ZoneAdminResponse {
    private UUID zoneId;
    private UUID parkingLotId;
    private String code;
    private String name;
    private UUID vehicleTypeId;
    private Integer capacity;
    private ZoneStatus status;
    private String createdAt;
    private UUID createdBy;
    private String updatedAt;
    private UUID updatedBy;
}