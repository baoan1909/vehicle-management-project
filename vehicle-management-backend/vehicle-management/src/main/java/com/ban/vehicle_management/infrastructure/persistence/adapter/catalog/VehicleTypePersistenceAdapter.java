package com.ban.vehicle_management.infrastructure.persistence.adapter.catalog;

import com.ban.vehicle_management.application.catalog.vehicletype.port.out.VehicleTypePortOut;
import com.ban.vehicle_management.domain.catalog.vehicletype.model.VehicleType;
import com.ban.vehicle_management.infrastructure.mapper.catalog.VehicleTypePersistenceMapper;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.catalog.VehicleTypeEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.catalog.VehicleTypeRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.specification.catalog.VehicleTypeSpecifications;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

@Component
public class VehicleTypePersistenceAdapter implements VehicleTypePortOut {

    private final VehicleTypeRepository vehicleTypeRepository;
    private final VehicleTypePersistenceMapper vehicleTypePersistenceMapper;

    public VehicleTypePersistenceAdapter(
            VehicleTypeRepository vehicleTypeRepository,
            VehicleTypePersistenceMapper vehicleTypePersistenceMapper
    ) {
        this.vehicleTypeRepository = vehicleTypeRepository;
        this.vehicleTypePersistenceMapper = vehicleTypePersistenceMapper;
    }

    @Override
    public VehicleType save(VehicleType vehicleType) {
        VehicleTypeEntity vehicleTypeEntity = vehicleTypePersistenceMapper.toEntity(vehicleType);
        VehicleTypeEntity savedVehicleTypeEntity = vehicleTypeRepository.saveAndFlush(vehicleTypeEntity);
        return vehicleTypePersistenceMapper.toDomain(savedVehicleTypeEntity);
    }

    @Override
    public Optional<VehicleType> findById(UUID vehicleTypeId) {
        return vehicleTypeRepository.findById(vehicleTypeId)
                .map(vehicleTypePersistenceMapper::toDomain);
    }

    @Override
    public List<VehicleType> findAll(Boolean isActive) {
        Specification<VehicleTypeEntity> specification = VehicleTypeSpecifications.withFilters(isActive);
        List<VehicleTypeEntity> vehicleTypeEntities = vehicleTypeRepository.findAll(specification);

        return vehicleTypeEntities.stream()
                .map(vehicleTypePersistenceMapper::toDomain)
                .toList();
    }

    @Override
    public boolean existsByCode(String code) {
        return vehicleTypeRepository.existsByCode(code);
    }

    @Override
    public boolean existsByCodeAndVehicleTypeIdNot(String code, UUID vehicleTypeId) {
        return vehicleTypeRepository.existsByCodeAndVehicleTypeIdNot(code, vehicleTypeId);
    }
}



