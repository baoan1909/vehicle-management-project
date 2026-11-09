package com.ban.vehicle_management.application.accesscontrol.card.port.in;

import com.ban.vehicle_management.application.accesscontrol.card.model.result.CardAdminView;
import com.ban.vehicle_management.shared.enumeration.accesscontrol.CardStatus;
import java.util.List;
import java.util.UUID;

public interface CardAdminQueryPortIn {

    CardAdminView getCardById(UUID cardId);

    List<CardAdminView> getCards(CardStatus status, UUID cardTypeId, UUID parkingLotId, String keyword);
}
