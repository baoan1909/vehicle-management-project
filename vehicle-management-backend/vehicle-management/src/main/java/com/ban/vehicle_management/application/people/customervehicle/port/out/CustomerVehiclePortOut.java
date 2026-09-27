package com.ban.vehicle_management.application.people.customervehicle.port.out;

import com.ban.vehicle_management.domain.people.customervehicle.model.CustomerVehicle;
import com.ban.vehicle_management.shared.enumeration.people.CustomerVehicleStatus;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface CustomerVehiclePortOut {

    CustomerVehicle save(CustomerVehicle customerVehicle);

    Optional<CustomerVehicle> findById(UUID customerVehicleId);

    Optional<CustomerVehicle> findByLicensePlate(String licensePlate);

    List<CustomerVehicle> findAll(
            UUID customerId,
            CustomerVehicleStatus status,
            UUID vehicleTypeId,
            Boolean isDefault,
            String keyword
    );

    List<CustomerVehicle> findAll(
            UUID customerId, CustomerVehicleStatus status, UUID vehicleTypeId,
            Boolean isDefault, String keyword, Set<UUID> parkingLotIds
    );

    boolean existsInParkingLots(UUID customerVehicleId, Set<UUID> parkingLotIds);

    boolean existsByLicensePlate(String licensePlate);

    boolean existsByLicensePlateAndCustomerVehicleIdNot(String licensePlate, UUID customerVehicleId);

    boolean existsCustomerById(UUID customerId);

    boolean existsVehicleTypeById(UUID vehicleTypeId);

    Optional<String> findVehicleTypeCodeById(UUID vehicleTypeId);

    boolean existsActiveSubscription(UUID customerVehicleId);

    boolean existsOpenParkingSession(UUID customerVehicleId);

    List<CustomerVehicle> findDefaultVehiclesByCustomerId(UUID customerId);
}

