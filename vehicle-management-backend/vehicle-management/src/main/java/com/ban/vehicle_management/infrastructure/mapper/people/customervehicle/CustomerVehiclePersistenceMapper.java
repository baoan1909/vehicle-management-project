package com.ban.vehicle_management.infrastructure.mapper.people.customervehicle;

import com.ban.vehicle_management.domain.people.customervehicle.model.CustomerVehicle;
import com.ban.vehicle_management.infrastructure.persistence.people.customervehicle.CustomerVehicleEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface CustomerVehiclePersistenceMapper {

    CustomerVehicleEntity toEntity(CustomerVehicle domain);

    CustomerVehicle toDomain(CustomerVehicleEntity entity);
}
