package com.ban.vehicle_management.entrypoint.dto.accesscontrol.card.request;

import com.ban.vehicle_management.shared.enumeration.accesscontrol.CardStatus;
import java.util.UUID;

public record CardFilterRequest(
        CardStatus status,
        UUID cardTypeId,
        String keyword
) {
}

