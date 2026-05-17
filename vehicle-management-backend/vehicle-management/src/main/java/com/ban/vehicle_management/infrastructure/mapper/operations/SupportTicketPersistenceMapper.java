package com.ban.vehicle_management.infrastructure.mapper.operations;

import com.ban.vehicle_management.domain.operations.supportticket.model.SupportTicket;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.operations.SupportTicketEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface SupportTicketPersistenceMapper {

    SupportTicketEntity toEntity(SupportTicket domain);

    SupportTicket toDomain(SupportTicketEntity entity);
}


