package com.ban.vehicle_management.infrastructure.mapper.iam.permission;

import com.ban.vehicle_management.domain.iam.permission.model.Permission;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.iam.PermissionEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface PermissionPersistenceMapper {

    PermissionEntity toEntity(Permission domain);

    Permission toDomain(PermissionEntity entity);
}

