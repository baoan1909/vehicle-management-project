package com.ban.vehicle_management.application.catalog.cardtype.port.in;

import com.ban.vehicle_management.domain.catalog.cardtype.model.CardType;
import java.util.List;
import java.util.UUID;

public interface CardTypePortIn {

    CardType createCardType(CardType cardType);

    CardType updateCardType(UUID cardTypeId, CardType cardType);

    CardType getCardTypeById(UUID cardTypeId);

    List<CardType> getCardTypes(Boolean isActive);

    void deleteCardType(UUID cardTypeId);
}

