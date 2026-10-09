package com.ban.vehicle_management.infrastructure.mapper.parking;

import com.ban.vehicle_management.domain.parking.parkinglevel.model.ParkingLevel;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.parking.ParkingLevelEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ParkingLevelPersistenceMapper {

    ParkingLevelEntity toEntity(ParkingLevel domain);

    ParkingLevel toDomain(ParkingLevelEntity entity);
}