package com.ban.vehicle_management.entrypoint.dto.catalog.tickettype.request;

public record CreateTicketTypeRequest(
        String code,
        String name,
        String description
) {
}
