package com.ban.vehicle_management.infrastructure.mapper.iam.role;

import com.ban.vehicle_management.domain.iam.role.model.Role;
import com.ban.vehicle_management.infrastructure.persistence.iam.role.RoleEntity;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-05-14T12:41:12+0700",
    comments = "version: 1.6.3, compiler: javac, environment: Java 23.0.2 (Oracle Corporation)"
)
@Component
public class RolePersistenceMapperImpl implements RolePersistenceMapper {

    @Override
    public RoleEntity toEntity(Role domain) {
        if ( domain == null ) {
            return null;
        }

        RoleEntity roleEntity = new RoleEntity();

        roleEntity.setCreatedAt( domain.getCreatedAt() );
        roleEntity.setCreatedBy( domain.getCreatedBy() );
        roleEntity.setUpdatedAt( domain.getUpdatedAt() );
        roleEntity.setUpdatedBy( domain.getUpdatedBy() );
        roleEntity.setRoleId( domain.getRoleId() );
        roleEntity.setCode( domain.getCode() );
        roleEntity.setName( domain.getName() );
        roleEntity.setDescription( domain.getDescription() );
        roleEntity.setIsSystem( domain.getIsSystem() );
        roleEntity.setIsActive( domain.getIsActive() );

        return roleEntity;
    }

    @Override
    public Role toDomain(RoleEntity entity) {
        if ( entity == null ) {
            return null;
        }

        Role role = new Role();

        role.setCreatedAt( entity.getCreatedAt() );
        role.setCreatedBy( entity.getCreatedBy() );
        role.setUpdatedAt( entity.getUpdatedAt() );
        role.setUpdatedBy( entity.getUpdatedBy() );
        role.setRoleId( entity.getRoleId() );
        role.setCode( entity.getCode() );
        role.setName( entity.getName() );
        role.setDescription( entity.getDescription() );
        role.setIsSystem( entity.getIsSystem() );
        role.setIsActive( entity.getIsActive() );

        return role;
    }
}
