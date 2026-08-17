package com.ban.vehicle_management.application.accesscontrol.card.port.in;

import com.ban.vehicle_management.domain.accesscontrol.card.model.Card;
import java.util.UUID;

public interface CardReclassificationPortIn {

    Card reclassifyCard(UUID cardId, UUID targetCardTypeId, String reason);
}
