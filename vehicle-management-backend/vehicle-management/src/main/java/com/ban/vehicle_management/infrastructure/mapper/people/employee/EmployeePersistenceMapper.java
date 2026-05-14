package com.ban.vehicle_management.infrastructure.mapper.people.employee;

import com.ban.vehicle_management.domain.people.employee.model.Employee;
import com.ban.vehicle_management.infrastructure.persistence.people.employee.EmployeeEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface EmployeePersistenceMapper {

    EmployeeEntity toEntity(Employee domain);

    Employee toDomain(EmployeeEntity entity);
}
