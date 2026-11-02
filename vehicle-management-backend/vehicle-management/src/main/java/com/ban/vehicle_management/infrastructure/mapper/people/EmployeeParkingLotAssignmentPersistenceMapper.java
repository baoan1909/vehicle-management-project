package com.ban.vehicle_management.infrastructure.mapper.people;

import com.ban.vehicle_management.domain.people.employee.model.EmployeeParkingLotAssignment;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.people.EmployeeParkingLotAssignmentEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface EmployeeParkingLotAssignmentPersistenceMapper {
    EmployeeParkingLotAssignmentEntity toEntity(EmployeeParkingLotAssignment domain);
    EmployeeParkingLotAssignment toDomain(EmployeeParkingLotAssignmentEntity entity);
}
