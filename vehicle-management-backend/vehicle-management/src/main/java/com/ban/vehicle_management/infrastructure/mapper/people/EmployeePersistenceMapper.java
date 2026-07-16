package com.ban.vehicle_management.infrastructure.mapper.people;

import com.ban.vehicle_management.domain.people.employee.model.Employee;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.people.EmployeeEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring", uses = UserProfilePersistenceMapper.class)
public interface EmployeePersistenceMapper {

    @Mapping(target = "userProfile", ignore = true)
    @Mapping(target = "shiftAssignments", ignore = true)
    EmployeeEntity toEntity(Employee domain);

    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "shiftAssignments", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "userProfile", ignore = true)
    void updateEntityFromDomain(Employee domain, @MappingTarget EmployeeEntity entity);

    @Mapping(target = "accountEmail", expression = "java(resolveAccountEmail(entity))")
    @Mapping(target = "accountUsername", expression = "java(resolveAccountUsername(entity))")
    @Mapping(target = "accountStatus", expression = "java(resolveAccountStatus(entity))")
    @Mapping(target = "roleCode", expression = "java(resolveRoleCode(entity))")
    @Mapping(target = "roleName", expression = "java(resolveRoleName(entity))")
    Employee toDomain(EmployeeEntity entity);

    default String resolveAccountEmail(EmployeeEntity entity) {
        if (entity == null
                || entity.getUserProfile() == null
                || entity.getUserProfile().getAccount() == null) {
            return null;
        }
        return entity.getUserProfile().getAccount().getEmail();
    }

    default String resolveAccountUsername(EmployeeEntity entity) {
        if (entity == null
                || entity.getUserProfile() == null
                || entity.getUserProfile().getAccount() == null) {
            return null;
        }
        return entity.getUserProfile().getAccount().getUsername();
    }

    default String resolveAccountStatus(EmployeeEntity entity) {
        if (entity == null
                || entity.getUserProfile() == null
                || entity.getUserProfile().getAccount() == null
                || entity.getUserProfile().getAccount().getStatus() == null) {
            return null;
        }
        return entity.getUserProfile().getAccount().getStatus().name();
    }

    default String resolveRoleCode(EmployeeEntity entity) {
        if (entity == null
                || entity.getUserProfile() == null
                || entity.getUserProfile().getAccount() == null
                || entity.getUserProfile().getAccount().getRole() == null) {
            return null;
        }
        return entity.getUserProfile().getAccount().getRole().getCode();
    }

    default String resolveRoleName(EmployeeEntity entity) {
        if (entity == null
                || entity.getUserProfile() == null
                || entity.getUserProfile().getAccount() == null
                || entity.getUserProfile().getAccount().getRole() == null) {
            return null;
        }
        return entity.getUserProfile().getAccount().getRole().getName();
    }
}


