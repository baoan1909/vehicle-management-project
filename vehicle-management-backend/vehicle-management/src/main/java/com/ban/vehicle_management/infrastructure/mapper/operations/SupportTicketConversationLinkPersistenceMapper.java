package com.ban.vehicle_management.infrastructure.mapper.operations;

import com.ban.vehicle_management.domain.operations.supportticket.model.SupportTicketConversationLink;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.operations.SupportTicketConversationLinkEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface SupportTicketConversationLinkPersistenceMapper {
    SupportTicketConversationLink toDomain(SupportTicketConversationLinkEntity entity);
    SupportTicketConversationLinkEntity toEntity(SupportTicketConversationLink domain);
}
