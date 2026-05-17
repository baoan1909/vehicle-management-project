package com.ban.vehicle_management.infrastructure.mapper.catalog;

import com.ban.vehicle_management.domain.catalog.cardtype.model.CardType;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.catalog.CardTypeEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface CardTypePersistenceMapper {

    CardTypeEntity toEntity(CardType domain);

    CardType toDomain(CardTypeEntity entity);
}


