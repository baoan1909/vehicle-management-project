package com.ban.vehicle_management.entrypoint.dto.catalog.tickettype.request;

import com.ban.vehicle_management.shared.enumeration.catalog.TicketTypeStatus;

public record TicketTypeFilterRequest (
        TicketTypeStatus status,
        String keyword
) {
}
