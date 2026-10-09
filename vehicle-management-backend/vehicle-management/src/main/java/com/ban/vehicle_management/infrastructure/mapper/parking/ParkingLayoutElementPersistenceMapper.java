package com.ban.vehicle_management.infrastructure.mapper.parking;

import com.ban.vehicle_management.domain.parking.parkinglayout.model.ParkingLayoutElement;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.parking.ParkingLayoutElementEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ParkingLayoutElementPersistenceMapper {

    ParkingLayoutElementEntity toEntity(ParkingLayoutElement domain);

    ParkingLayoutElement toDomain(ParkingLayoutElementEntity entity);
}