package com.ban.vehicle_management.entrypoint.dto.accesscontrol.card.request;

import java.util.UUID;

public record CreateCardRequest(
        String cardNumber,
        String uid,
        UUID cardTypeId
) {
}
