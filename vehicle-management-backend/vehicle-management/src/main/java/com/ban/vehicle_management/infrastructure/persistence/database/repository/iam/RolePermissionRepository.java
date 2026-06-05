package com.ban.vehicle_management.infrastructure.persistence.database.repository.iam;

import com.ban.vehicle_management.infrastructure.persistence.database.entity.iam.RolePermissionEntity;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RolePermissionRepository extends JpaRepository<RolePermissionEntity, UUID> {

    @Query(value = """
    select rp.*
    from iam.role_permissions rp
    where rp.role_id = :roleId
    """, nativeQuery = true)
    List<RolePermissionEntity> findByRoleId(@Param("roleId") UUID roleId);

    @Query(value = """
    select rp.*
    from iam.role_permissions rp
    where rp.role_id = :roleId
      and rp.permission_id = :permissionId
    """, nativeQuery = true)
    Optional<RolePermissionEntity> findByRoleIdAndPermissionId(
            @Param("roleId") UUID roleId,
            @Param("permissionId") UUID permissionId
    );

    @Query(value = """
    select distinct p.permission_code
    from iam.role_permissions rp
    join iam.permissions p on rp.permission_id = p.permission_id
    join iam.roles r on rp.role_id = r.role_id
    where rp.role_id = :roleId
      and rp.is_active = true
      and r.is_active = true
    """, nativeQuery = true)
    Set<String> findActivePermissionCodesByRoleId(@Param("roleId") UUID roleId);
}


