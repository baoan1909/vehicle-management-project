package com.ban.vehicle_management.infrastructure.mapper.catalog.cardtype;

import com.ban.vehicle_management.domain.catalog.cardtype.model.CardType;
import com.ban.vehicle_management.infrastructure.persistence.catalog.cardtype.CardTypeEntity;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-05-14T12:41:12+0700",
    comments = "version: 1.6.3, compiler: javac, environment: Java 23.0.2 (Oracle Corporation)"
)
@Component
public class CardTypePersistenceMapperImpl implements CardTypePersistenceMapper {

    @Override
    public CardTypeEntity toEntity(CardType domain) {
        if ( domain == null ) {
            return null;
        }

        CardTypeEntity cardTypeEntity = new CardTypeEntity();

        cardTypeEntity.setCreatedAt( domain.getCreatedAt() );
        cardTypeEntity.setCreatedBy( domain.getCreatedBy() );
        cardTypeEntity.setUpdatedAt( domain.getUpdatedAt() );
        cardTypeEntity.setUpdatedBy( domain.getUpdatedBy() );
        cardTypeEntity.setCardTypeId( domain.getCardTypeId() );
        cardTypeEntity.setCode( domain.getCode() );
        cardTypeEntity.setName( domain.getName() );
        cardTypeEntity.setDescription( domain.getDescription() );
        cardTypeEntity.setIsReturnRequired( domain.getIsReturnRequired() );

        return cardTypeEntity;
    }

    @Override
    public CardType toDomain(CardTypeEntity entity) {
        if ( entity == null ) {
            return null;
        }

        CardType cardType = new CardType();

        cardType.setCreatedAt( entity.getCreatedAt() );
        cardType.setCreatedBy( entity.getCreatedBy() );
        cardType.setUpdatedAt( entity.getUpdatedAt() );
        cardType.setUpdatedBy( entity.getUpdatedBy() );
        cardType.setCardTypeId( entity.getCardTypeId() );
        cardType.setCode( entity.getCode() );
        cardType.setName( entity.getName() );
        cardType.setDescription( entity.getDescription() );
        cardType.setIsReturnRequired( entity.getIsReturnRequired() );

        return cardType;
    }
}
