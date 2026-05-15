package com.ban.vehicle_management.domain.people.customervehicle.policy;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.ban.vehicle_management.domain.people.customervehicle.model.CustomerVehicle;
import com.ban.vehicle_management.shared.enumeration.CustomerVehicleStatus;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CustomerVehiclePolicyTest {

    private final CustomerVehiclePolicy customerVehiclePolicy = new CustomerVehiclePolicy();

    @Test
    void shouldInitializeCustomerVehicleWithDefaults() {
        CustomerVehicle customerVehicle = new CustomerVehicle();
        customerVehicle.setCustomerId(UUID.randomUUID());
        customerVehicle.setVehicleTypeId(UUID.randomUUID());
        customerVehicle.setLicensePlate(" 51A-12345 ");
        customerVehicle.setBrand(" Honda ");
        customerVehicle.setColor(" White ");

        customerVehiclePolicy.initialize(customerVehicle);

        assertEquals("51A-12345", customerVehicle.getLicensePlate());
        assertEquals("Honda", customerVehicle.getBrand());
        assertEquals("White", customerVehicle.getColor());
        assertEquals(Boolean.FALSE, customerVehicle.getIsDefault());
        assertEquals(CustomerVehicleStatus.ACTIVE, customerVehicle.getStatus());
    }

    @Test
    void shouldBlockCustomerVehicle() {
        CustomerVehicle customerVehicle = new CustomerVehicle();
        customerVehicle.setCustomerId(UUID.randomUUID());
        customerVehicle.setVehicleTypeId(UUID.randomUUID());
        customerVehicle.setLicensePlate("51A-12345");
        customerVehicle.setStatus(CustomerVehicleStatus.ACTIVE);
        customerVehicle.setIsDefault(Boolean.FALSE);

        customerVehiclePolicy.block(customerVehicle);

        assertEquals(CustomerVehicleStatus.BLOCKED, customerVehicle.getStatus());
    }
}

