package com.ban.vehicle_management.infrastructure.mapper.iam.role;

import com.ban.vehicle_management.domain.iam.role.model.RolePermission;
import com.ban.vehicle_management.infrastructure.persistence.iam.role.RolePermissionEntity;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-05-14T12:41:12+0700",
    comments = "version: 1.6.3, compiler: javac, environment: Java 23.0.2 (Oracle Corporation)"
)
@Component
public class RolePermissionPersistenceMapperImpl implements RolePermissionPersistenceMapper {

    @Override
    public RolePermissionEntity toEntity(RolePermission domain) {
        if ( domain == null ) {
            return null;
        }

        RolePermissionEntity rolePermissionEntity = new RolePermissionEntity();

        rolePermissionEntity.setCreatedAt( domain.getCreatedAt() );
        rolePermissionEntity.setCreatedBy( domain.getCreatedBy() );
        rolePermissionEntity.setUpdatedAt( domain.getUpdatedAt() );
        rolePermissionEntity.setUpdatedBy( domain.getUpdatedBy() );
        rolePermissionEntity.setId( domain.getId() );
        rolePermissionEntity.setRoleId( domain.getRoleId() );
        rolePermissionEntity.setPermissionId( domain.getPermissionId() );

        return rolePermissionEntity;
    }

    @Override
    public RolePermission toDomain(RolePermissionEntity entity) {
        if ( entity == null ) {
            return null;
        }

        RolePermission rolePermission = new RolePermission();

        rolePermission.setCreatedAt( entity.getCreatedAt() );
        rolePermission.setCreatedBy( entity.getCreatedBy() );
        rolePermission.setUpdatedAt( entity.getUpdatedAt() );
        rolePermission.setUpdatedBy( entity.getUpdatedBy() );
        rolePermission.setId( entity.getId() );
        rolePermission.setRoleId( entity.getRoleId() );
        rolePermission.setPermissionId( entity.getPermissionId() );

        return rolePermission;
    }
}
