package com.ban.vehicle_management.infrastructure.mapper.accesscontrol.card;

import com.ban.vehicle_management.domain.accesscontrol.card.model.Card;
import com.ban.vehicle_management.infrastructure.persistence.accesscontrol.card.CardEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface CardPersistenceMapper {

    CardEntity toEntity(Card domain);

    Card toDomain(CardEntity entity);
}
