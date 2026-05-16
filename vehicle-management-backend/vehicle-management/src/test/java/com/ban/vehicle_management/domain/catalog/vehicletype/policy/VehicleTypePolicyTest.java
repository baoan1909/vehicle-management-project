package com.ban.vehicle_management.domain.catalog.vehicletype.policy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.ban.vehicle_management.domain.catalog.vehicletype.model.VehicleType;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import org.junit.jupiter.api.Test;

class VehicleTypePolicyTest {

    private final VehicleTypePolicy vehicleTypePolicy = new VehicleTypePolicy();

    @Test
    void shouldInitializeVehicleTypeWithDefaults() {
        VehicleType vehicleType = new VehicleType();
        vehicleType.setCode(" CAR ");
        vehicleType.setName(" O to ");
        vehicleType.setDescription(" Mo ta ");

        vehicleTypePolicy.initialize(vehicleType);

        assertEquals("CAR", vehicleType.getCode());
        assertEquals("O to", vehicleType.getName());
        assertEquals("Mo ta", vehicleType.getDescription());
        assertEquals(Boolean.TRUE, vehicleType.getIsActive());
    }

    @Test
    void shouldRejectBlankCode() {
        VehicleType vehicleType = new VehicleType();
        vehicleType.setCode(" ");
        vehicleType.setName("Xe may");

        assertThrows(BadRequestException.class, () -> vehicleTypePolicy.initialize(vehicleType));
    }

    @Test
    void shouldDeactivateVehicleType() {
        VehicleType vehicleType = new VehicleType();
        vehicleType.setIsActive(Boolean.TRUE);

        vehicleTypePolicy.deactivate(vehicleType);

        assertFalse(vehicleType.getIsActive());
    }
}

