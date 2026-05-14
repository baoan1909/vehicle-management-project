package com.ban.vehicle_management.infrastructure.mapper.parking.parkingsession;

import com.ban.vehicle_management.domain.parking.parkingsession.model.ParkingSession;
import com.ban.vehicle_management.infrastructure.persistence.parking.parkingsession.ParkingSessionEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ParkingSessionPersistenceMapper {

    ParkingSessionEntity toEntity(ParkingSession domain);

    ParkingSession toDomain(ParkingSessionEntity entity);
}
