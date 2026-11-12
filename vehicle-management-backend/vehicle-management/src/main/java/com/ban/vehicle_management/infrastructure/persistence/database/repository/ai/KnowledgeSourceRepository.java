package com.ban.vehicle_management.infrastructure.persistence.database.repository.ai;

import com.ban.vehicle_management.infrastructure.persistence.database.entity.ai.KnowledgeSourceEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Collection;
import java.time.Instant;

public interface KnowledgeSourceRepository extends JpaRepository<KnowledgeSourceEntity, UUID>, JpaSpecificationExecutor<KnowledgeSourceEntity> {

    boolean existsByTitleIgnoreCase(String title);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
        update ai.knowledge_sources
           set status = 'INACTIVE', updated_at = :now, updated_by_account_id = :actorAccountId
         where tenant_id in (:tenantIds) and access_scope = 'TENANT_PRIVATE' and status = 'ACTIVE'
        """, nativeQuery = true)
    int deactivateTenantPrivate(
            @Param("tenantIds") Collection<UUID> tenantIds,
            @Param("actorAccountId") UUID actorAccountId,
            @Param("now") Instant now);
}
