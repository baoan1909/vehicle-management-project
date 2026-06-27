package com.ban.vehicle_management.infrastructure.mapper.operations;

import com.ban.vehicle_management.domain.operations.shift.model.Shift;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.operations.ShiftEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ShiftPersistenceMapper {

    @Mapping(target = "shiftTemplate", ignore = true)
    @Mapping(target = "parkingLot", ignore = true)
    @Mapping(target = "shiftAssignments", ignore = true)
    ShiftEntity toEntity(Shift shift);

    Shift toDomain(ShiftEntity entity);
}