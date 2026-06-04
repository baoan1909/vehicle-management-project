package com.ban.vehicle_management.application.catalog.tickettype.port.in;

import com.ban.vehicle_management.domain.catalog.tickettype.model.TicketType;
import com.ban.vehicle_management.shared.enumeration.catalog.TicketTypeStatus;

import java.util.List;
import java.util.UUID;

public interface TicketTypePortIn {
    TicketType createTicketType(TicketType ticketType);

    TicketType getTicketTypeById(UUID ticketTypeId);

    List<TicketType> getTicketTypes(TicketTypeStatus status, String keywork);

    TicketType updateTicketType(UUID ticketTypeId, TicketType ticketType);

    void deleteTicketType(UUID ticketTypeId);

    TicketType activateTicketType(UUID ticketTypeId);
}
