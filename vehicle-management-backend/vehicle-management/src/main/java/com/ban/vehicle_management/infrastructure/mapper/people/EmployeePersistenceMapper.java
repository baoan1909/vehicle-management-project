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

    @Mapping(target = "accountEmail", expression = "java(resolveAccountEmail(entity))")
    Employee toDomain(EmployeeEntity entity);

    default String resolveAccountEmail(EmployeeEntity entity) {
        if (entity == null
                || entity.getUserProfile() == null
                || entity.getUserProfile().getAccount() == null) {
            return null;
        }
        return entity.getUserProfile().getAccount().getEmail();
    }
}


