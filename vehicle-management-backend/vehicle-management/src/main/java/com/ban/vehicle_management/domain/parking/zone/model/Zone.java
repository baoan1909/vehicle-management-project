package com.ban.vehicle_management.domain.parking.zone.model;

import com.ban.vehicle_management.domain.common.model.AuditableDomainModel;
import com.ban.vehicle_management.shared.enumeration.parking.ZoneStatus;
import java.util.UUID;
import java.util.HashSet;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Zone extends AuditableDomainModel {

    private UUID zoneId;
    private UUID parkingLotId;
    private String code;
    private String name;
    private UUID vehicleTypeId;
    private Set<UUID> vehicleTypeIds = new HashSet<>();
    private Integer capacity;
    private ZoneStatus status;
}
