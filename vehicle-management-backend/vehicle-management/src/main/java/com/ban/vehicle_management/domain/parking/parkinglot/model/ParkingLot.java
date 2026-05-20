package com.ban.vehicle_management.domain.parking.parkinglot.model;

import com.ban.vehicle_management.domain.common.model.AuditableDomainModel;
import com.ban.vehicle_management.shared.enumeration.parking.ParkingLotStatus;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ParkingLot extends AuditableDomainModel {

    private UUID parkingLotId;
    private String code;
    private String name;
    private String address;
    private Integer totalCapacity;
    private ParkingLotStatus status;
}

