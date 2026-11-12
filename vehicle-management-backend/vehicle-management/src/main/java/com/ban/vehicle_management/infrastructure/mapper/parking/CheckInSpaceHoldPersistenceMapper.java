package com.ban.vehicle_management.infrastructure.mapper.parking;

import com.ban.vehicle_management.domain.parking.checkinspacehold.model.CheckInSpaceHold;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.parking.CheckInSpaceHoldEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface CheckInSpaceHoldPersistenceMapper {
    CheckInSpaceHoldEntity toEntity(CheckInSpaceHold domain);
    CheckInSpaceHold toDomain(CheckInSpaceHoldEntity entity);
}
