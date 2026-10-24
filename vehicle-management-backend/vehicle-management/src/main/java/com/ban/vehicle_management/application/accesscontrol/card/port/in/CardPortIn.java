package com.ban.vehicle_management.application.accesscontrol.card.port.in;

import com.ban.vehicle_management.domain.accesscontrol.card.model.Card;
import com.ban.vehicle_management.shared.enumeration.accesscontrol.CardStatus;
import java.util.List;
import java.util.UUID;

public interface CardPortIn {

    Card createCard(Card card);

    Card getCardById(UUID cardId);

    default List<Card> getCards(CardStatus status, UUID cardTypeId, String keyword) {
        return getCards(status, cardTypeId, null, keyword);
    }

    List<Card> getCards(CardStatus status, UUID cardTypeId, UUID parkingLotId, String keyword);

    Card updateCard(UUID cardId, Card card);

    void deleteCard(UUID cardId);
}

