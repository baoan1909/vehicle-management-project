package com.ban.vehicle_management.domain.parking.parkinglevel.model;

import com.ban.vehicle_management.domain.common.model.AuditableDomainModel;
import com.ban.vehicle_management.shared.enumeration.parking.ParkingLevelStatus;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ParkingLevel extends AuditableDomainModel {

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
}