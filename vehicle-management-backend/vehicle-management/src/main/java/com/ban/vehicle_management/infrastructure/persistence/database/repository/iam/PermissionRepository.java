package com.ban.vehicle_management.infrastructure.persistence.database.repository.iam;

import com.ban.vehicle_management.infrastructure.persistence.database.entity.iam.PermissionEntity;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PermissionRepository extends JpaRepository<PermissionEntity, UUID>, JpaSpecificationExecutor<PermissionEntity> {

    @Query(value = """
        select p.*
        from iam.permissions p
        where p.permission_id in (:permissionIds)
        """, nativeQuery = true)
    List<PermissionEntity> findByPermissionIdIn(@Param("permissionIds") Collection<UUID> permissionIds);

    @Query(value = """
        select distinct p.*
        from iam.permissions p
        join iam.role_permissions rp on rp.permission_id = p.permission_id
        where rp.role_id = :roleId
          and rp.is_active = true
        order by p.permission_code
        """, nativeQuery = true)
    List<PermissionEntity> findActiveByRoleId(@Param("roleId") UUID roleId);
}


