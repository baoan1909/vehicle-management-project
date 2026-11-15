package com.ban.vehicle_management.domain.catalog.vehicletype.policy;

import com.ban.vehicle_management.domain.catalog.vehicletype.model.VehicleType;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import com.ban.vehicle_management.shared.enumeration.catalog.CatalogLifecycleStatus;
import com.ban.vehicle_management.shared.utils.TextValidationUtils;

public class VehicleTypePolicy {

    public void initialize(VehicleType vehicleType) {
        requireVehicleType(vehicleType);
        vehicleType.setCode(TextValidationUtils.normalizeCode(vehicleType.getCode(), "code", 50));
        vehicleType.setName(TextValidationUtils.normalizeRequiredText(vehicleType.getName(), "name", 100));
        vehicleType.setDescription(TextValidationUtils.normalizeNullableText(vehicleType.getDescription(), "description", 0));
        if (vehicleType.getIsActive() == null) {
            vehicleType.setLifecycleStatus(CatalogLifecycleStatus.DRAFT);
        }
    }

    public void deactivate(VehicleType vehicleType) {
        requireVehicleType(vehicleType);
        vehicleType.setLifecycleStatus(CatalogLifecycleStatus.INACTIVE);
    }

    public void activate(VehicleType vehicleType) {
        requireVehicleType(vehicleType);
        vehicleType.setLifecycleStatus(CatalogLifecycleStatus.ACTIVE);
        initialize(vehicleType);
    }

    private void requireVehicleType(VehicleType vehicleType) {
        if (vehicleType == null) {
            throw new BadRequestException("vehicleType must not be null");
        }
    }
}

