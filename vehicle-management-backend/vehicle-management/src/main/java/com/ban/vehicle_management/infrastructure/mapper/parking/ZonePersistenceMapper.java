package com.ban.vehicle_management.infrastructure.mapper.parking;

import com.ban.vehicle_management.domain.parking.zone.model.Zone;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.parking.ZoneEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ZonePersistenceMapper {

    ZoneEntity toEntity(Zone domain);

    Zone toDomain(ZoneEntity entity);
}