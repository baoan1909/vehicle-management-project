package com.ban.vehicle_management.application.catalog.vehicletype.port.in;

import com.ban.vehicle_management.domain.catalog.vehicletype.model.VehicleType;
import java.util.List;
import java.util.UUID;

public interface VehicleTypeUseCase {

    VehicleType createVehicleType(VehicleType vehicleType);

    VehicleType updateVehicleType(UUID vehicleTypeId, VehicleType vehicleType);

    VehicleType getVehicleTypeById(UUID vehicleTypeId);

    List<VehicleType> getVehicleTypes(Boolean isActive);

    void deleteVehicleType(UUID vehicleTypeId);
}

