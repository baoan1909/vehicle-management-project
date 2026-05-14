package com.ban.vehicle_management.infrastructure.mapper.parking.parkingevent;

import com.ban.vehicle_management.domain.parking.parkingevent.model.ParkingEvent;
import com.ban.vehicle_management.infrastructure.persistence.parking.parkingevent.ParkingEventEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ParkingEventPersistenceMapper {

    ParkingEventEntity toEntity(ParkingEvent domain);

    ParkingEvent toDomain(ParkingEventEntity entity);
}
