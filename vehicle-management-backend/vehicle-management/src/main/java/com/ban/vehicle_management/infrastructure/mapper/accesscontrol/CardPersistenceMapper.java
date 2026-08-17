package com.ban.vehicle_management.infrastructure.mapper.accesscontrol;

import com.ban.vehicle_management.domain.accesscontrol.card.model.Card;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.accesscontrol.CardEntity;
import java.util.List;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface CardPersistenceMapper {

    CardEntity toEntity(Card domain);

    Card toDomain(CardEntity entity);

    List<CardEntity> toEntities(List<Card> domains);

    List<Card> toDomains(List<CardEntity> entities);
}


