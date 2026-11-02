package com.ban.vehicle_management.infrastructure.mapper.iam;

import com.ban.vehicle_management.domain.iam.organization.model.ParkingLotManagementAssignment;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.iam.ParkingLotManagementAssignmentEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ParkingLotManagementAssignmentPersistenceMapper {
    ParkingLotManagementAssignmentEntity toEntity(ParkingLotManagementAssignment domain);
    ParkingLotManagementAssignment toDomain(ParkingLotManagementAssignmentEntity entity);
}
