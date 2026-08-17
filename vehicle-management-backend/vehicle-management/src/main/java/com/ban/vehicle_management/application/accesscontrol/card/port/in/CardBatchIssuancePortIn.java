package com.ban.vehicle_management.application.accesscontrol.card.port.in;

import com.ban.vehicle_management.domain.accesscontrol.card.model.Card;
import java.util.List;
import java.util.UUID;

public interface CardBatchIssuancePortIn {

    List<Card> createCards(UUID cardTypeId, Integer quantity);
}
