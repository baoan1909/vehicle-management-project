package com.ban.vehicle_management.infrastructure.mapper.operations;

import com.ban.vehicle_management.domain.operations.supportticket.model.SupportTicketEvent;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.operations.SupportTicketEventEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface SupportTicketEventPersistenceMapper {
    SupportTicketEventEntity toEntity(SupportTicketEvent domain);
    SupportTicketEvent toDomain(SupportTicketEventEntity entity);
}
