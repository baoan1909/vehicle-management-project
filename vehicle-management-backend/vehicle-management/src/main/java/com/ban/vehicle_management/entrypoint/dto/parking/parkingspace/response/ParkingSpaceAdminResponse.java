package com.ban.vehicle_management.entrypoint.dto.parking.parkingspace.response;

import com.ban.vehicle_management.shared.enumeration.parking.ManualOccupancyType;
import com.ban.vehicle_management.shared.enumeration.parking.ParkingSpaceStatus;
import com.ban.vehicle_management.shared.enumeration.parking.ParkingSpaceStatusSource;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ParkingSpaceAdminResponse {
    private UUID parkingSpaceId;
    private UUID zoneId;
    private String code;
    private UUID vehicleTypeId;
    private ParkingSpaceStatus status;
    private String lifecycleStatus;
    private ParkingSpaceStatusSource statusSource;
    private ManualOccupancyType manualOccupancyType;
    private String statusReason;
    private Long version;
    private BigDecimal x;
    private BigDecimal y;
    private BigDecimal width;
    private BigDecimal height;
    private BigDecimal rotation;
    private Instant createdAt;
    private UUID createdBy;
    private Instant updatedAt;
    private UUID updatedBy;
}
