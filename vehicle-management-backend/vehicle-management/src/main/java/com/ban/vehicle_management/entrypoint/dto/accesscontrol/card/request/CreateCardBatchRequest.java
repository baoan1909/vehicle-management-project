package com.ban.vehicle_management.entrypoint.dto.accesscontrol.card.request;

import java.util.UUID;

public record CreateCardBatchRequest(
        UUID cardTypeId,
        Integer quantity
) {
}
