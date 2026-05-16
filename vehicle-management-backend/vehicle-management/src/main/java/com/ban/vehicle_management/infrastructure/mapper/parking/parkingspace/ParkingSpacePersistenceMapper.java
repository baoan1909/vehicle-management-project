package com.ban.vehicle_management.infrastructure.mapper.parking.parkingspace;

import com.ban.vehicle_management.domain.parking.parkingspace.model.ParkingSpace;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.parking.ParkingSpaceEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ParkingSpacePersistenceMapper {

    ParkingSpaceEntity toEntity(ParkingSpace domain);

    ParkingSpace toDomain(ParkingSpaceEntity entity);
}

