package com.ban.vehicle_management.infrastructure.persistence.database.repository.catalog;

import com.ban.vehicle_management.infrastructure.persistence.database.entity.catalog.VehicleTypeEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface VehicleTypeRepository extends JpaRepository<VehicleTypeEntity, UUID>, JpaSpecificationExecutor<VehicleTypeEntity> {

    boolean existsByCode(String code);

    boolean existsByCodeAndOrganizationId(String code, UUID organizationId);

    boolean existsByCodeAndVehicleTypeIdNot(String code, UUID vehicleTypeId);

    boolean existsByCodeAndOrganizationIdAndVehicleTypeIdNot(String code, UUID organizationId, UUID vehicleTypeId);

    boolean existsByVehicleTypeIdAndIsActiveTrue(UUID vehicleTypeId);
}


