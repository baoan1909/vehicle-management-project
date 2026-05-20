package com.ban.vehicle_management.infrastructure.persistence.database.repository.people;

import com.ban.vehicle_management.infrastructure.persistence.database.entity.people.CustomerVehicleEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface CustomerVehicleRepository
        extends JpaRepository<CustomerVehicleEntity, UUID>, JpaSpecificationExecutor<CustomerVehicleEntity> {

    boolean existsByLicensePlate(String licensePlate);

    boolean existsByLicensePlateAndCustomerVehicleIdNot(String licensePlate, UUID customerVehicleId);

    List<CustomerVehicleEntity> findByCustomerIdAndIsDefaultTrue(UUID customerId);
}


