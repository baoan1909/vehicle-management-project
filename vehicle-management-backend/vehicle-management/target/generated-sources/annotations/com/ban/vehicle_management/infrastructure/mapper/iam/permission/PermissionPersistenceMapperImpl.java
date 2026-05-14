package com.ban.vehicle_management.infrastructure.mapper.iam.permission;

import com.ban.vehicle_management.domain.iam.permission.model.Permission;
import com.ban.vehicle_management.infrastructure.persistence.iam.permission.PermissionEntity;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-05-14T12:41:12+0700",
    comments = "version: 1.6.3, compiler: javac, environment: Java 23.0.2 (Oracle Corporation)"
)
@Component
public class PermissionPersistenceMapperImpl implements PermissionPersistenceMapper {

    @Override
    public PermissionEntity toEntity(Permission domain) {
        if ( domain == null ) {
            return null;
        }

        PermissionEntity permissionEntity = new PermissionEntity();

        permissionEntity.setCreatedAt( domain.getCreatedAt() );
        permissionEntity.setCreatedBy( domain.getCreatedBy() );
        permissionEntity.setUpdatedAt( domain.getUpdatedAt() );
        permissionEntity.setUpdatedBy( domain.getUpdatedBy() );
        permissionEntity.setPermissionId( domain.getPermissionId() );
        permissionEntity.setPermissionCode( domain.getPermissionCode() );
        permissionEntity.setModule( domain.getModule() );
        permissionEntity.setAction( domain.getAction() );
        permissionEntity.setName( domain.getName() );
        permissionEntity.setDescription( domain.getDescription() );

        return permissionEntity;
    }

    @Override
    public Permission toDomain(PermissionEntity entity) {
        if ( entity == null ) {
            return null;
        }

        Permission permission = new Permission();

        permission.setCreatedAt( entity.getCreatedAt() );
        permission.setCreatedBy( entity.getCreatedBy() );
        permission.setUpdatedAt( entity.getUpdatedAt() );
        permission.setUpdatedBy( entity.getUpdatedBy() );
        permission.setPermissionId( entity.getPermissionId() );
        permission.setPermissionCode( entity.getPermissionCode() );
        permission.setModule( entity.getModule() );
        permission.setAction( entity.getAction() );
        permission.setName( entity.getName() );
        permission.setDescription( entity.getDescription() );

        return permission;
    }
}
