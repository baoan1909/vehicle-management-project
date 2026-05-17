package com.ban.vehicle_management.infrastructure.mapper.catalog;

import com.ban.vehicle_management.domain.catalog.priceplan.model.PricePlan;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.catalog.PricePlanEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface PricePlanPersistenceMapper {

    PricePlanEntity toEntity(PricePlan domain);

    PricePlan toDomain(PricePlanEntity entity);
}


