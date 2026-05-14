package com.ban.vehicle_management.domain.people.customervehicle.model;

import com.ban.vehicle_management.domain.common.model.AuditableDomainModel;
import com.ban.vehicle_management.shared.enumeration.CustomerVehicleStatus;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CustomerVehicle extends AuditableDomainModel {

    private UUID customerVehicleId;
    private UUID customerId;
    private UUID vehicleTypeId;
    private String licensePlate;
    private String brand;
    private String color;
    private Boolean isDefault;
    private CustomerVehicleStatus status;
}
