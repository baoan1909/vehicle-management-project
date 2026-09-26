package com.ban.vehicle_management.infrastructure.persistence.database.repository.catalog;

import com.ban.vehicle_management.infrastructure.persistence.database.entity.catalog.VehicleTypeEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface VehicleTypeRepository extends JpaRepository<VehicleTypeEntity, UUID>, JpaSpecificationExecutor<VehicleTypeEntity> {

    boolean existsByCode(String code);

    boolean existsByCodeAndVehicleTypeIdNot(String code, UUID vehicleTypeId);

    boolean existsByVehicleTypeIdAndIsActiveTrue(UUID vehicleTypeId);

    @Query("select vehicleType.code from VehicleTypeEntity vehicleType where vehicleType.vehicleTypeId = :vehicleTypeId")
    Optional<String> findCodeById(@Param("vehicleTypeId") UUID vehicleTypeId);
}


