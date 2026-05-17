package com.ban.vehicle_management.infrastructure.mapper.people;

import com.ban.vehicle_management.domain.people.customervehicle.model.CustomerVehicle;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.people.CustomerVehicleEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface CustomerVehiclePersistenceMapper {

    CustomerVehicleEntity toEntity(CustomerVehicle domain);

    CustomerVehicle toDomain(CustomerVehicleEntity entity);
}


