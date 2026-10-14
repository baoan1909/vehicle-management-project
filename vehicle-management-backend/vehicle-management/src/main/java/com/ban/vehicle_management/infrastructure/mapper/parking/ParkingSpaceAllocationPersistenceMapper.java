package com.ban.vehicle_management.infrastructure.mapper.parking;

import com.ban.vehicle_management.domain.parking.parkingspaceallocation.model.ParkingSpaceAllocation;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.parking.ParkingSpaceAllocationEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ParkingSpaceAllocationPersistenceMapper {

    ParkingSpaceAllocationEntity toEntity(ParkingSpaceAllocation domain);

    ParkingSpaceAllocation toDomain(ParkingSpaceAllocationEntity entity);
}