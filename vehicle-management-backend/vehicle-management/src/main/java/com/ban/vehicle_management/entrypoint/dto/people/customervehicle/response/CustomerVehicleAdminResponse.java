package com.ban.vehicle_management.entrypoint.dto.people.customervehicle.response;

import com.ban.vehicle_management.shared.enumeration.people.CustomerVehicleStatus;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class CustomerVehicleAdminResponse {

    private UUID customerVehicleId;
    private UUID customerId;
    private UUID vehicleTypeId;
    private String licensePlate;
    private String brand;
    private String color;
    private Boolean isDefault;
    private CustomerVehicleStatus status;
    private String createdAt;
    private UUID createdBy;
    private String updatedAt;
    private UUID updatedBy;
}

