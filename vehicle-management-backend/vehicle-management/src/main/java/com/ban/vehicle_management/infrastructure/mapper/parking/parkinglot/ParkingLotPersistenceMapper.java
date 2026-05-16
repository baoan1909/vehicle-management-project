package com.ban.vehicle_management.infrastructure.mapper.parking.parkinglot;

import com.ban.vehicle_management.domain.parking.parkinglot.model.ParkingLot;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.parking.ParkingLotEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ParkingLotPersistenceMapper {

    ParkingLotEntity toEntity(ParkingLot domain);

    ParkingLot toDomain(ParkingLotEntity entity);
}

