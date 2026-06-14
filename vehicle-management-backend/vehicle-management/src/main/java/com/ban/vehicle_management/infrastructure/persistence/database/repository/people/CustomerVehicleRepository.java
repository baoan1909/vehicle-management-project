package com.ban.vehicle_management.infrastructure.persistence.database.repository.people;

import com.ban.vehicle_management.infrastructure.persistence.database.entity.people.CustomerVehicleEntity;
import com.ban.vehicle_management.shared.enumeration.people.CustomerVehicleStatus;
import java.util.Collection;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface CustomerVehicleRepository
        extends JpaRepository<CustomerVehicleEntity, UUID>, JpaSpecificationExecutor<CustomerVehicleEntity> {

    java.util.Optional<CustomerVehicleEntity> findByLicensePlate(String licensePlate);

    boolean existsByLicensePlate(String licensePlate);

    boolean existsByLicensePlateAndCustomerVehicleIdNot(String licensePlate, UUID customerVehicleId);

    boolean existsByVehicleTypeIdAndStatusIn(UUID vehicleTypeId, Collection<CustomerVehicleStatus> statuses);

    List<CustomerVehicleEntity> findByCustomerIdAndIsDefaultTrue(UUID customerId);
}


