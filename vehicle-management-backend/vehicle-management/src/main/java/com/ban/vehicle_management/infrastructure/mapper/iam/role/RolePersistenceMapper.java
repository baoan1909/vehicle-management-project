package com.ban.vehicle_management.infrastructure.mapper.iam.role;

import com.ban.vehicle_management.domain.iam.role.model.Role;
import com.ban.vehicle_management.infrastructure.persistence.iam.role.RoleEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface RolePersistenceMapper {

    RoleEntity toEntity(Role domain);

    Role toDomain(RoleEntity entity);
}
