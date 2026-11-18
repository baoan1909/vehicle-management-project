package com.ban.vehicle_management.infrastructure.mapper.operations;

import com.ban.vehicle_management.domain.operations.supportticketcategory.model.SupportTicketCategory;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.operations.SupportTicketCategoryEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface SupportTicketCategoryPersistenceMapper {
    SupportTicketCategoryEntity toEntity(SupportTicketCategory domain);

    @Mapping(target = "requiredPermissionCodes", ignore = true)
    SupportTicketCategory toDomain(SupportTicketCategoryEntity entity);
}
