package com.ban.vehicle_management.infrastructure.mapper.people;

import com.ban.vehicle_management.domain.people.employee.model.Employee;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.people.EmployeeEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = UserProfilePersistenceMapper.class)
public interface EmployeePersistenceMapper {

    @Mapping(target = "userProfile", ignore = true)
    @Mapping(target = "shiftAssignments", ignore = true)
    EmployeeEntity toEntity(Employee domain);

    Employee toDomain(EmployeeEntity entity);
}


