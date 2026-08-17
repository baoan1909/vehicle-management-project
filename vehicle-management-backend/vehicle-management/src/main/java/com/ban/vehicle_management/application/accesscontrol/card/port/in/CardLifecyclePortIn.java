package com.ban.vehicle_management.application.accesscontrol.card.port.in;

import com.ban.vehicle_management.domain.accesscontrol.card.model.Card;
import java.util.UUID;

public interface CardLifecyclePortIn {

    Card blockCard(UUID cardId, String reason);

    Card unblockCard(UUID cardId);

    Card retireCard(UUID cardId, String reason);

    Card recoverLostCard(UUID cardId, String inspectionNote);
}
