package com.ban.vehicle_management.infrastructure.mapper.catalog;

import com.ban.vehicle_management.domain.catalog.pricerule.model.PriceRule;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.catalog.PriceRuleEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface PriceRulePersistenceMapper {

    PriceRuleEntity toEntity(PriceRule domain);

    PriceRule toDomain(PriceRuleEntity entity);
}


