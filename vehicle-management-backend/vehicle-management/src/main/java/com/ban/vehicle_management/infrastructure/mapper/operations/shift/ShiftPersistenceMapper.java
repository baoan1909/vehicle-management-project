package com.ban.vehicle_management.infrastructure.mapper.operations.shift;

import com.ban.vehicle_management.domain.operations.shift.model.Shift;
import com.ban.vehicle_management.infrastructure.persistence.operations.shift.ShiftEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ShiftPersistenceMapper {

    ShiftEntity toEntity(Shift domain);

    Shift toDomain(ShiftEntity entity);
}
