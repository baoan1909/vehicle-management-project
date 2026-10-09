package com.ban.vehicle_management.domain.parking.parkinglayout.model;

import com.ban.vehicle_management.domain.common.model.AuditableDomainModel;
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
public class ParkingSpaceLayoutItem extends AuditableDomainModel {

    private UUID layoutItemId;
    private UUID layoutVersionId;
    private UUID parkingSpaceId;
    private BigDecimal x;
    private BigDecimal y;
    private BigDecimal width;
    private BigDecimal height;
    private BigDecimal rotation;
}