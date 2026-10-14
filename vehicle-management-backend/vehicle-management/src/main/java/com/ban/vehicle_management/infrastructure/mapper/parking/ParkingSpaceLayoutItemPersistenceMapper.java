package com.ban.vehicle_management.infrastructure.mapper.parking;

import com.ban.vehicle_management.domain.parking.parkinglayout.model.ParkingSpaceLayoutItem;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.parking.ParkingSpaceLayoutItemEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ParkingSpaceLayoutItemPersistenceMapper {

    ParkingSpaceLayoutItemEntity toEntity(ParkingSpaceLayoutItem domain);

    ParkingSpaceLayoutItem toDomain(ParkingSpaceLayoutItemEntity entity);
}