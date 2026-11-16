package com.ban.vehicle_management.entrypoint.dto.catalog.vehicletype.response;


import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class VehicleTypeUserResponse {

    private UUID vehicleTypeId;
    private UUID canonicalVehicleTypeId;
    private String code;
    private String name;
    private String description;
    private Boolean isActive;
}
