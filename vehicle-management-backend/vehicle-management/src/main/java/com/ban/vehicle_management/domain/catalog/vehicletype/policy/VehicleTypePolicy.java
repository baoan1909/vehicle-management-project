package com.ban.vehicle_management.domain.catalog.vehicletype.policy;

import com.ban.vehicle_management.domain.catalog.vehicletype.model.VehicleType;
import com.ban.vehicle_management.shared.exception.BadRequestException;

public class VehicleTypePolicy {

    public void initialize(VehicleType vehicleType) {
        requireVehicleType(vehicleType);
        vehicleType.setCode(normalizeRequired(vehicleType.getCode(), "code"));
        vehicleType.setName(normalizeRequired(vehicleType.getName(), "name"));
        vehicleType.setDescription(normalizeNullable(vehicleType.getDescription()));
        if (vehicleType.getIsActive() == null) {
            vehicleType.setIsActive(Boolean.TRUE);
        }
    }

    public void deactivate(VehicleType vehicleType) {
        requireVehicleType(vehicleType);
        vehicleType.setIsActive(Boolean.FALSE);
    }

    private void requireVehicleType(VehicleType vehicleType) {
        if (vehicleType == null) {
            throw new BadRequestException("vehicleType must not be null");
        }
    }

    private String normalizeRequired(String value, String fieldName) {
        String normalizedValue = normalizeNullable(value);
        if (normalizedValue == null) {
            throw new BadRequestException(fieldName + " must not be blank");
        }
        return normalizedValue;
    }

    private String normalizeNullable(String value) {
        if (value == null) {
            return null;
        }

        String normalizedValue = value.trim();
        return normalizedValue.isEmpty() ? null : normalizedValue;
    }
}

