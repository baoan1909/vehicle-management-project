package com.ban.vehicle_management.infrastructure.mapper.catalog.tickettype;

import com.ban.vehicle_management.domain.catalog.tickettype.model.TicketType;
import com.ban.vehicle_management.infrastructure.persistence.catalog.tickettype.TicketTypeEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface TicketTypePersistenceMapper {

    TicketTypeEntity toEntity(TicketType domain);

    TicketType toDomain(TicketTypeEntity entity);
}
