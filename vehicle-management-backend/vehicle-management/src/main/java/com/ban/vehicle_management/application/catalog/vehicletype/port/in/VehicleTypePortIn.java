package com.ban.vehicle_management.application.catalog.vehicletype.port.in;

import com.ban.vehicle_management.application.catalog.vehicletype.model.VehicleTypeDeactivationImpact;
import com.ban.vehicle_management.domain.catalog.vehicletype.model.VehicleType;
import java.util.List;
import java.util.UUID;

public interface VehicleTypePortIn {

    VehicleType createVehicleType(VehicleType vehicleType);

    VehicleType createVehicleType(VehicleType vehicleType, UUID parkingLotId);

    VehicleType updateVehicleType(UUID vehicleTypeId, VehicleType vehicleType);

    VehicleType updateVehicleType(UUID vehicleTypeId, VehicleType vehicleType, UUID parkingLotId);

    VehicleType getVehicleTypeById(UUID vehicleTypeId);

    List<VehicleType> getVehicleTypes(Boolean isActive);

    void deleteVehicleType(UUID vehicleTypeId);

    void deactivateVehicleType(UUID vehicleTypeId, boolean cascadePriceRules);

    VehicleTypeDeactivationImpact getDeactivationImpact(UUID vehicleTypeId);

    VehicleType activateVehicleType(UUID vehicleTypeId);
}

