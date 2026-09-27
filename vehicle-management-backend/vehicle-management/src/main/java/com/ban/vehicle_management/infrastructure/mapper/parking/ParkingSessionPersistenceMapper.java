package com.ban.vehicle_management.infrastructure.mapper.parking;

import com.ban.vehicle_management.domain.parking.parkingsession.model.ParkingSession;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.parking.ParkingSessionEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ParkingSessionPersistenceMapper {

    @Mapping(target = "licensePlateInNormalized", ignore = true)
    @Mapping(target = "licensePlateOutNormalized", ignore = true)
    ParkingSessionEntity toEntity(ParkingSession domain);

    @Mapping(target = "licensePlateIn", expression = "java(entity.getLicensePlateInNormalized() == null ? entity.getLicensePlateIn() : entity.getLicensePlateInNormalized())")
    @Mapping(target = "licensePlateOut", expression = "java(entity.getLicensePlateOutNormalized() == null ? entity.getLicensePlateOut() : entity.getLicensePlateOutNormalized())")
    ParkingSession toDomain(ParkingSessionEntity entity);
}


