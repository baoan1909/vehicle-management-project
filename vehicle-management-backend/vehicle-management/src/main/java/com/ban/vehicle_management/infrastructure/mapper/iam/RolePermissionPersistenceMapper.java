package com.ban.vehicle_management.infrastructure.mapper.iam;

import com.ban.vehicle_management.domain.iam.role.model.RolePermission;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.iam.RolePermissionEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface RolePermissionPersistenceMapper {

    RolePermissionEntity toEntity(RolePermission domain);

    RolePermission toDomain(RolePermissionEntity entity);
}


