package com.ban.vehicle_management.domain.people.customervehicle.policy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.ban.vehicle_management.domain.people.customervehicle.model.CustomerVehicle;
import com.ban.vehicle_management.shared.enumeration.people.CustomerVehicleStatus;
import com.ban.vehicle_management.shared.exception.BadRequestException;
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
        customerVehicle.setIsDefault(Boolean.TRUE);

        customerVehiclePolicy.block(customerVehicle);

        assertEquals(CustomerVehicleStatus.BLOCKED, customerVehicle.getStatus());
        assertEquals(Boolean.FALSE, customerVehicle.getIsDefault());
    }

    @Test
    void shouldRejectBrandExceedingSchemaLength() {
        CustomerVehicle customerVehicle = new CustomerVehicle();
        customerVehicle.setCustomerId(UUID.randomUUID());
        customerVehicle.setVehicleTypeId(UUID.randomUUID());
        customerVehicle.setLicensePlate("51A-12345");
        customerVehicle.setBrand("A".repeat(81));

        assertThrows(BadRequestException.class, () -> customerVehiclePolicy.initialize(customerVehicle));
    }

    @Test
    void shouldRejectMarkingInactiveVehicleAsDefault() {
        CustomerVehicle customerVehicle = new CustomerVehicle();
        customerVehicle.setCustomerId(UUID.randomUUID());
        customerVehicle.setVehicleTypeId(UUID.randomUUID());
        customerVehicle.setLicensePlate("51A-12345");
        customerVehicle.setStatus(CustomerVehicleStatus.INACTIVE);
        customerVehicle.setIsDefault(Boolean.FALSE);

        assertThrows(BadRequestException.class, () -> customerVehiclePolicy.markDefault(customerVehicle));
    }
}

