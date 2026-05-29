package com.ban.vehicle_management.infrastructure.mapper.parking;

import com.ban.vehicle_management.domain.parking.gate.model.Gate;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.parking.GateEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface GatePersistenceMapper {

    GateEntity toEntity(Gate domain);

    Gate toDomain(GateEntity entity);
}