package com.ban.vehicle_management.infrastructure.mapper.operations.shift;

import com.ban.vehicle_management.domain.operations.shift.model.ShiftAssignment;
import com.ban.vehicle_management.infrastructure.persistence.operations.shift.ShiftAssignmentEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ShiftAssignmentPersistenceMapper {

    ShiftAssignmentEntity toEntity(ShiftAssignment domain);

    ShiftAssignment toDomain(ShiftAssignmentEntity entity);
}
