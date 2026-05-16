package com.ban.vehicle_management.entrypoint.dto.catalog.vehicletype.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class UpdateVehicleTypeRequest {

    private String code;
    private String name;
    private String description;
    private Boolean isActive;
}

