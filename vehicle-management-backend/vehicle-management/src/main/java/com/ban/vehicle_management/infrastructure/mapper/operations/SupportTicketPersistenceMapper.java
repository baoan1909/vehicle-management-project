package com.ban.vehicle_management.infrastructure.mapper.operations;

import com.ban.vehicle_management.domain.operations.supportticket.model.SupportTicket;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.operations.SupportTicketEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface SupportTicketPersistenceMapper {

    @Mapping(target = "customer", ignore = true)
    @Mapping(target = "category", ignore = true)
    @Mapping(target = "assignedToAccount", ignore = true)
    @Mapping(target = "closedByAccount", ignore = true)
    SupportTicketEntity toEntity(SupportTicket domain);

    @Mapping(target = "categoryCode", source = "category.code")
    @Mapping(target = "categoryName", source = "category.name")
    @Mapping(target = "priority", source = "category.priority")
    SupportTicket toDomain(SupportTicketEntity entity);
}


