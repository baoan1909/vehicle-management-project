package com.ban.vehicle_management.infrastructure.mapper.operations;

import com.ban.vehicle_management.domain.operations.supportticketcategory.model.SupportTicketCategory;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.operations.SupportTicketCategoryEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface SupportTicketCategoryPersistenceMapper {
    SupportTicketCategoryEntity toEntity(SupportTicketCategory domain);

    SupportTicketCategory toDomain(SupportTicketCategoryEntity entity);
}
