package com.ban.vehicle_management.entrypoint.dto.accesscontrol.card.request;

import com.ban.vehicle_management.shared.enumeration.accesscontrol.CardStatus;

public record ChangeCardStatusRequest(
        CardStatus status,
        String blockedReason
) {
}

