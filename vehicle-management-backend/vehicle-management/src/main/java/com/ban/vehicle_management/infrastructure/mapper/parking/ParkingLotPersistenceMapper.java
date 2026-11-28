package com.ban.vehicle_management.infrastructure.mapper.parking;

import com.ban.vehicle_management.infrastructure.persistence.database.repository.parking.ParkingLotResponsibilityProjection;

import com.ban.vehicle_management.domain.parking.parkinglot.model.ParkingLotResponsibility;

import com.ban.vehicle_management.domain.parking.parkinglot.model.ParkingLot;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.parking.ParkingLotEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ParkingLotPersistenceMapper {
    ParkingLotResponsibility.Manager toResponsibleManager(ParkingLotResponsibilityProjection row);

    ParkingLotEntity toEntity(ParkingLot domain);

    ParkingLot toDomain(ParkingLotEntity entity);
}