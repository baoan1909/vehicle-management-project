package com.ban.vehicle_management.entrypoint.dto.catalog.vehicletype.response;

import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class VehicleTypeAdminResponse {

    private UUID vehicleTypeId;
    private String code;
    private String name;
    private String description;
    private Boolean isActive;
    private String createdAt;
    private UUID createdBy;
    private String updatedAt;
    private UUID updatedBy;
}

