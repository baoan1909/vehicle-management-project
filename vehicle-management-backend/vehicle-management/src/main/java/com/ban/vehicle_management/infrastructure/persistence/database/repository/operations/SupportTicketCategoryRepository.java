package com.ban.vehicle_management.infrastructure.persistence.database.repository.operations;

import com.ban.vehicle_management.infrastructure.persistence.database.entity.operations.SupportTicketCategoryEntity;
import com.ban.vehicle_management.shared.enumeration.operations.SupportTicketCategoryStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface SupportTicketCategoryRepository
    extends JpaRepository<SupportTicketCategoryEntity, UUID>, JpaSpecificationExecutor<SupportTicketCategoryEntity> {
    boolean existsByCodeAndStatus(String code, SupportTicketCategoryStatus status);

    boolean existsByCode(String code);

    boolean existsByCodeAndCategoryIdNot(String code, UUID categoryId);

    boolean existsByCodeAndStatusAndCategoryIdNot(
            String code,
            SupportTicketCategoryStatus status,
            UUID categoryId
    );

    boolean existsByCategoryIdAndStatus(UUID categoryId, SupportTicketCategoryStatus status);

    @Query(value = """
            SELECT permission.permission_code
            FROM operations.support_ticket_category_permissions mapping
            JOIN iam.permissions permission ON permission.permission_id = mapping.permission_id
            WHERE mapping.category_id = :categoryId
            ORDER BY permission.permission_code
            """, nativeQuery = true)
    List<String> findRequiredPermissionCodes(@Param("categoryId") UUID categoryId);

    @Modifying
    @Query(value = "DELETE FROM operations.support_ticket_category_permissions WHERE category_id = :categoryId",
            nativeQuery = true)
    void deleteRequiredPermissionMappings(@Param("categoryId") UUID categoryId);

    @Modifying
    @Query(value = """
            INSERT INTO operations.support_ticket_category_permissions (category_permission_id, category_id, permission_id)
            SELECT gen_random_uuid(), :categoryId, permission.permission_id
            FROM iam.permissions permission
            WHERE permission.permission_code IN (:permissionCodes)
            ON CONFLICT (category_id, permission_id) DO NOTHING
            """, nativeQuery = true)
    void insertRequiredPermissionMappings(
            @Param("categoryId") UUID categoryId,
            @Param("permissionCodes") Collection<String> permissionCodes
    );

}
