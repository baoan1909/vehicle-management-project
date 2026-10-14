package com.ban.vehicle_management.entrypoint.dto.parking.parkinglevel.response;

import com.ban.vehicle_management.shared.enumeration.parking.ParkingLevelStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ParkingLevelAdminResponse {
    private UUID parkingLevelId;
    private UUID parkingLotId;
    private String code;
    private String name;
    private Integer displayOrder;
    private BigDecimal elevation;
    private BigDecimal floorHeight;
    private BigDecimal canvasWidth;
    private BigDecimal canvasHeight;
    private ParkingLevelStatus status;
    private Instant createdAt;
    private UUID createdBy;
    private Instant updatedAt;
    private UUID updatedBy;
}
