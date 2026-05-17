package com.ban.vehicle_management.infrastructure.mapper.catalog;

import com.ban.vehicle_management.domain.catalog.vehicletype.model.VehicleType;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.catalog.VehicleTypeEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface VehicleTypePersistenceMapper {

    VehicleTypeEntity toEntity(VehicleType domain);

    VehicleType toDomain(VehicleTypeEntity entity);
}


