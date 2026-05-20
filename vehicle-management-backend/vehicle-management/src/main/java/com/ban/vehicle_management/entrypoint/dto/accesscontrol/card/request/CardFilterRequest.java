package com.ban.vehicle_management.entrypoint.dto.accesscontrol.card.request;

import com.ban.vehicle_management.shared.enumeration.CardStatus;
import java.util.UUID;

public record CardFilterRequest(
        CardStatus status,
        UUID cardTypeId,
        UUID vehicleTypeId,
        String keyword
) {
}
