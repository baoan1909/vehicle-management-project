package com.ban.vehicle_management.infrastructure.mapper.people;

import com.ban.vehicle_management.domain.people.customervehicle.model.CustomerVehicle;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.people.CustomerVehicleEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CustomerVehiclePersistenceMapper {

    @Mapping(target = "licensePlateNormalized", ignore = true)
    CustomerVehicleEntity toEntity(CustomerVehicle domain);

    @Mapping(target = "licensePlate", expression = "java(entity.getLicensePlateNormalized() == null ? entity.getLicensePlate() : entity.getLicensePlateNormalized())")
    CustomerVehicle toDomain(CustomerVehicleEntity entity);
}


