package com.ban.vehicle_management.infrastructure.mapper.operations;

import com.ban.vehicle_management.domain.operations.employeerosterrule.model.EmployeeRosterRule;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.operations.EmployeeRosterRuleEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface EmployeeRosterRulePersistenceMapper {

    @Mapping(target = "parkingLot", ignore = true)
    @Mapping(target = "employee", ignore = true)
    @Mapping(target = "preferredGate", ignore = true)
    EmployeeRosterRuleEntity toEntity(EmployeeRosterRule rule);

    EmployeeRosterRule toDomain(EmployeeRosterRuleEntity entity);
}