package com.ban.vehicle_management.application.catalog.vehicletype.port.out;

import com.ban.vehicle_management.domain.catalog.vehicletype.model.VehicleType;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.time.LocalDate;

public interface VehicleTypePortOut {

    VehicleType save(VehicleType vehicleType);

    Optional<VehicleType> findById(UUID vehicleTypeId);

    List<VehicleType> findAll(Boolean isActive);

    List<VehicleType> findAll(Boolean isActive, Set<UUID> organizationIds);

    boolean existsByCode(String code);

    boolean existsByCodeInOrganization(String code, UUID organizationId);

    boolean existsByCodeAndVehicleTypeIdNot(String code, UUID vehicleTypeId);

    boolean existsByCodeInOrganizationExcludingId(String code, UUID organizationId, UUID vehicleTypeId);

    long countActivePriceRules(UUID vehicleTypeId);

    long countActiveCustomerVehicles(UUID vehicleTypeId);

    long countOpenParkingSessions(UUID vehicleTypeId);

    long countAssignedZones(UUID vehicleTypeId);

    int deactivateActivePriceRules(UUID vehicleTypeId);

    int detachVehicleTypeFromZones(UUID vehicleTypeId);

    long countServiceObligationSubscriptions(UUID parkingLotId, UUID vehicleTypeId, LocalDate businessDate);

    long countServiceObligationSubscriptions(UUID vehicleTypeId, LocalDate businessDate);

    long countOpenParkingSessions(UUID parkingLotId, UUID vehicleTypeId);

    int detachVehicleTypeFromZones(UUID parkingLotId, UUID vehicleTypeId);
}

