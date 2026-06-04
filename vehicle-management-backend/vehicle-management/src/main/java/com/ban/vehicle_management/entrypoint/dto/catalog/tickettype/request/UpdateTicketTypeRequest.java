package com.ban.vehicle_management.entrypoint.dto.catalog.tickettype.request;

public record UpdateTicketTypeRequest (
        String code,
        String name,
        String description
){
}
