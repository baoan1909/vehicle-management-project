package com.ban.vehicle_management.entrypoint.dto.parking.parkinglayout.response;

import java.math.BigDecimal;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class LayoutSpaceItemResponse {
    private UUID layoutItemId;
    private UUID layoutVersionId;
    private UUID parkingSpaceId;
    private String spaceCode;
    private UUID vehicleTypeId;
    private BigDecimal x;
    private BigDecimal y;
    private BigDecimal width;
    private BigDecimal height;
    private BigDecimal rotation;
}
