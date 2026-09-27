package com.ban.vehicle_management.entrypoint.dto.parking.zone.response;

import java.time.Instant;

import com.ban.vehicle_management.shared.enumeration.parking.ZoneStatus;
import java.util.UUID;
import java.util.Set;
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
    private Set<UUID> vehicleTypeIds;
    private Integer capacity;
    private ZoneStatus status;
    private Instant createdAt;
    private UUID createdBy;
    private Instant updatedAt;
    private UUID updatedBy;
}
