package com.ban.vehicle_management.infrastructure.mapper.operations;

import com.ban.vehicle_management.domain.operations.shiftassignment.model.ShiftAssignment;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.operations.ShiftAssignmentEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ShiftAssignmentPersistenceMapper {

    @Mapping(target = "shift", ignore = true)
    @Mapping(target = "employee", ignore = true)
    @Mapping(target = "gate", ignore = true)
    ShiftAssignmentEntity toEntity(ShiftAssignment assignment);

    ShiftAssignment toDomain(ShiftAssignmentEntity entity);
}