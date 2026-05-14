package com.ban.vehicle_management.infrastructure.mapper.accesscontrol.card;

import com.ban.vehicle_management.domain.accesscontrol.card.model.Card;
import com.ban.vehicle_management.infrastructure.persistence.accesscontrol.card.CardEntity;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-05-14T12:41:12+0700",
    comments = "version: 1.6.3, compiler: javac, environment: Java 23.0.2 (Oracle Corporation)"
)
@Component
public class CardPersistenceMapperImpl implements CardPersistenceMapper {

    @Override
    public CardEntity toEntity(Card domain) {
        if ( domain == null ) {
            return null;
        }

        CardEntity cardEntity = new CardEntity();

        cardEntity.setCreatedAt( domain.getCreatedAt() );
        cardEntity.setCreatedBy( domain.getCreatedBy() );
        cardEntity.setUpdatedAt( domain.getUpdatedAt() );
        cardEntity.setUpdatedBy( domain.getUpdatedBy() );
        cardEntity.setCardId( domain.getCardId() );
        cardEntity.setCardNumber( domain.getCardNumber() );
        cardEntity.setUid( domain.getUid() );
        cardEntity.setCardTypeId( domain.getCardTypeId() );
        cardEntity.setVehicleTypeId( domain.getVehicleTypeId() );
        cardEntity.setStatus( domain.getStatus() );
        cardEntity.setIssuedAt( domain.getIssuedAt() );
        cardEntity.setBlockedAt( domain.getBlockedAt() );
        cardEntity.setBlockedReason( domain.getBlockedReason() );

        return cardEntity;
    }

    @Override
    public Card toDomain(CardEntity entity) {
        if ( entity == null ) {
            return null;
        }

        Card card = new Card();

        card.setCreatedAt( entity.getCreatedAt() );
        card.setCreatedBy( entity.getCreatedBy() );
        card.setUpdatedAt( entity.getUpdatedAt() );
        card.setUpdatedBy( entity.getUpdatedBy() );
        card.setCardId( entity.getCardId() );
        card.setCardNumber( entity.getCardNumber() );
        card.setUid( entity.getUid() );
        card.setCardTypeId( entity.getCardTypeId() );
        card.setVehicleTypeId( entity.getVehicleTypeId() );
        card.setStatus( entity.getStatus() );
        card.setIssuedAt( entity.getIssuedAt() );
        card.setBlockedAt( entity.getBlockedAt() );
        card.setBlockedReason( entity.getBlockedReason() );

        return card;
    }
}
