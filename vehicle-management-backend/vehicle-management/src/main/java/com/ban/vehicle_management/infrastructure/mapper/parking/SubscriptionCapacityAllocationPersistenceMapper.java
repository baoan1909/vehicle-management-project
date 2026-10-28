package com.ban.vehicle_management.infrastructure.mapper.parking;

import com.ban.vehicle_management.domain.parking.capacityallocation.model.SubscriptionCapacityAllocation;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.parking.SubscriptionCapacityAllocationEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface SubscriptionCapacityAllocationPersistenceMapper {
    SubscriptionCapacityAllocation toDomain(SubscriptionCapacityAllocationEntity entity);
    SubscriptionCapacityAllocationEntity toEntity(SubscriptionCapacityAllocation domain);
}

