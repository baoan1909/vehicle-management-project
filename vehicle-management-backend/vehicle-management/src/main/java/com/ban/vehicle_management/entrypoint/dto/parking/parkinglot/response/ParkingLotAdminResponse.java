package com.ban.vehicle_management.entrypoint.dto.parking.parkinglot.response;

import com.ban.vehicle_management.shared.enumeration.parking.ParkingLotStatus;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ParkingLotAdminResponse {
    private UUID parkingLotId;
    private String code;
    private String name;
    private String address;
    private Integer totalCapacity;
    private ParkingLotStatus status;
    private String createdAt;
    private UUID createdBy;
    private String updatedAt;
    private UUID updatedBy;
}