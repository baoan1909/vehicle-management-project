package com.ban.vehicle_management.infrastructure.mapper.parking;

import com.ban.vehicle_management.domain.parking.lane.model.Lane;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.parking.LaneEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface LanePersistenceMapper {

    LaneEntity toEntity(Lane domain);

    Lane toDomain(LaneEntity entity);
}


