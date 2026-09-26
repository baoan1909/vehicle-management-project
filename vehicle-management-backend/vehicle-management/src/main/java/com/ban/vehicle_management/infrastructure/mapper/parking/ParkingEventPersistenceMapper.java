package com.ban.vehicle_management.infrastructure.mapper.parking;

import com.ban.vehicle_management.domain.parking.parkingevent.model.ParkingEvent;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.parking.ParkingEventEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ParkingEventPersistenceMapper {

    @Mapping(target = "licensePlateDetectedNormalized", ignore = true)
    ParkingEventEntity toEntity(ParkingEvent domain);

    @Mapping(target = "licensePlateDetected", expression = "java(entity.getLicensePlateDetectedNormalized() == null ? entity.getLicensePlateDetected() : entity.getLicensePlateDetectedNormalized())")
    ParkingEvent toDomain(ParkingEventEntity entity);
}


