package com.ban.vehicle_management.application.catalog.vehicletype.port.out;

import com.ban.vehicle_management.domain.catalog.vehicletype.model.VehicleType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VehicleTypePortOut {

    VehicleType save(VehicleType vehicleType);

    Optional<VehicleType> findById(UUID vehicleTypeId);

    List<VehicleType> findAll(Boolean isActive);

    boolean existsByCode(String code);

    boolean existsByCodeAndVehicleTypeIdNot(String code, UUID vehicleTypeId);

    boolean hasActivePriceRules(UUID vehicleTypeId);

    boolean hasActiveCustomerVehicles(UUID vehicleTypeId);

    boolean hasOpenParkingSessions(UUID vehicleTypeId);

    boolean hasActiveCards(UUID vehicleTypeId);

    boolean hasActiveZones(UUID vehicleTypeId);
}

