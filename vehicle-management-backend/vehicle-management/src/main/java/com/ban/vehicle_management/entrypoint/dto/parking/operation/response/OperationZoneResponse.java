package com.ban.vehicle_management.entrypoint.dto.parking.operation.response;


import com.ban.vehicle_management.shared.enumeration.parking.LayoutStatus;
import com.ban.vehicle_management.shared.enumeration.parking.TrackingMode;
import com.ban.vehicle_management.shared.enumeration.parking.ZoneStatus;
import java.util.UUID;
import java.util.Set;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class OperationZoneResponse {
    private UUID zoneId;
    private UUID parkingLotId;
    private UUID parkingLevelId;
    private String code;
    private String name;
    private UUID vehicleTypeId;
    private Set<UUID> vehicleTypeIds;
    private Integer capacity;
    private Integer unavailableCapacity;
    private TrackingMode trackingMode;
    private LayoutStatus layoutStatus;
    private Long layoutVersion;
    private ZoneStatus status;




}
