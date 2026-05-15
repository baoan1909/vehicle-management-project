package com.ban.vehicle_management.infrastructure.persistence.database.repository.catalog;

import com.ban.vehicle_management.infrastructure.persistence.database.entity.catalog.VehicleTypeEntity;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VehicleTypeRepository extends JpaRepository<VehicleTypeEntity, UUID> {

    boolean existsByCode(String code);

    boolean existsByCodeAndVehicleTypeIdNot(String code, UUID vehicleTypeId);

    List<VehicleTypeEntity> findAllByOrderByCodeAsc();

    List<VehicleTypeEntity> findAllByIsActiveOrderByCodeAsc(Boolean isActive);
}


