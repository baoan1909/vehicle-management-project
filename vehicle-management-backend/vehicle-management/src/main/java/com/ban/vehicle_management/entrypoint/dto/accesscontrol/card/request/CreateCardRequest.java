package com.ban.vehicle_management.entrypoint.dto.accesscontrol.card.request;

import java.util.UUID;

public record CreateCardRequest(
        UUID cardTypeId,
        UUID parkingLotId
) {
}
