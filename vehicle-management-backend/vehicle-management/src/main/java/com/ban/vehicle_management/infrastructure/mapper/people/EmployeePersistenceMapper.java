package com.ban.vehicle_management.infrastructure.mapper.people;

import com.ban.vehicle_management.domain.people.employee.model.Employee;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.people.EmployeeEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface EmployeePersistenceMapper {

    EmployeeEntity toEntity(Employee domain);

    Employee toDomain(EmployeeEntity entity);
}


