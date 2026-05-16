package com.ban.vehicle_management.application.accesscontrol.card.port.in;

import com.ban.vehicle_management.domain.accesscontrol.card.model.Card;
import com.ban.vehicle_management.shared.enumeration.CardStatus;
import java.util.UUID;

public interface ChangeCardStatusUseCase {

    Card changeCardStatus(UUID cardId, CardStatus status, String blockedReason);
}
