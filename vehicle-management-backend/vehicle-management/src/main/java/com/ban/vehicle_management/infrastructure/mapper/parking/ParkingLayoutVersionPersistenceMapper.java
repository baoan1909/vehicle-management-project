package com.ban.vehicle_management.infrastructure.mapper.parking;

import com.ban.vehicle_management.domain.parking.parkinglayout.model.ParkingLayoutVersion;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.parking.ParkingLayoutVersionEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ParkingLayoutVersionPersistenceMapper {

    ParkingLayoutVersionEntity toEntity(ParkingLayoutVersion domain);

    ParkingLayoutVersion toDomain(ParkingLayoutVersionEntity entity);
}