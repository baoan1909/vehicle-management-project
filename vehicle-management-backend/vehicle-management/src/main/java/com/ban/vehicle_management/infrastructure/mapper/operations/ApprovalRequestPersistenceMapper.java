package com.ban.vehicle_management.infrastructure.mapper.operations;

import com.ban.vehicle_management.domain.operations.approvalrequest.model.ApprovalRequest;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.operations.ApprovalRequestEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ApprovalRequestPersistenceMapper {

    ApprovalRequestEntity toEntity(ApprovalRequest domain);

    ApprovalRequest toDomain(ApprovalRequestEntity entity);
}


