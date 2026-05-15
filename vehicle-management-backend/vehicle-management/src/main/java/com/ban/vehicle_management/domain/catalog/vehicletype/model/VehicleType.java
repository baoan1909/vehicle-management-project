package com.ban.vehicle_management.domain.catalog.vehicletype.model;

import com.ban.vehicle_management.domain.common.model.AuditableDomainModel;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class VehicleType extends AuditableDomainModel {

    private UUID vehicleTypeId;
    private String code;
    private String name;
    private String description;
    private Boolean isActive;
}

