package com.ban.vehicle_management.application.people.customervehicle.port.in;

import com.ban.vehicle_management.domain.people.customervehicle.model.CustomerVehicle;
import com.ban.vehicle_management.shared.enumeration.CustomerVehicleStatus;
import java.util.List;
import java.util.UUID;

public interface CustomerVehiclePortIn {

    CustomerVehicle createCustomerVehicle(CustomerVehicle customerVehicle);

    CustomerVehicle updateCustomerVehicle(UUID customerVehicleId, CustomerVehicle customerVehicle);

    CustomerVehicle getCustomerVehicleById(UUID customerVehicleId);

    List<CustomerVehicle> getCustomerVehicles(
            UUID customerId,
            CustomerVehicleStatus status,
            UUID vehicleTypeId,
            Boolean isDefault,
            String keyword
    );

    void deleteCustomerVehicle(UUID customerVehicleId);

    CustomerVehicle activateCustomerVehicle(UUID customerVehicleId);

    CustomerVehicle inactivateCustomerVehicle(UUID customerVehicleId);

    CustomerVehicle blockCustomerVehicle(UUID customerVehicleId);

    CustomerVehicle markCustomerVehicleAsDefault(UUID customerVehicleId);

    CustomerVehicle unmarkCustomerVehicleAsDefault(UUID customerVehicleId);
}
