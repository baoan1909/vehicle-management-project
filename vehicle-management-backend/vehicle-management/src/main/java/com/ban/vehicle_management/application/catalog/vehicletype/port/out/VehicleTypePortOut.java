package com.ban.vehicle_management.application.catalog.vehicletype.port.out;

import com.ban.vehicle_management.domain.catalog.vehicletype.model.VehicleType;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface VehicleTypePortOut {

    VehicleType save(VehicleType vehicleType);

    Optional<VehicleType> findById(UUID vehicleTypeId);

    List<VehicleType> findAll(Boolean isActive);

    List<VehicleType> findAll(Boolean isActive, Set<UUID> organizationIds);

    boolean existsByCode(String code);

    boolean existsByCodeInOrganization(String code, UUID organizationId);

    boolean existsByCodeAndVehicleTypeIdNot(String code, UUID vehicleTypeId);

    boolean existsByCodeInOrganizationExcludingId(String code, UUID organizationId, UUID vehicleTypeId);

    boolean hasActivePriceRules(UUID vehicleTypeId);

    boolean hasActiveCustomerVehicles(UUID vehicleTypeId);

    boolean hasOpenParkingSessions(UUID vehicleTypeId);

    boolean hasActiveZones(UUID vehicleTypeId);
}

