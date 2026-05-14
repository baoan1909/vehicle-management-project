package com.ban.vehicle_management.domain.parking.parkingspace.model;

import com.ban.vehicle_management.domain.common.model.AuditableDomainModel;
import com.ban.vehicle_management.shared.enumeration.ParkingSpaceStatus;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ParkingSpace extends AuditableDomainModel {

    private UUID parkingSpaceId;
    private UUID zoneId;
    private String code;
    private ParkingSpaceStatus status;
}
