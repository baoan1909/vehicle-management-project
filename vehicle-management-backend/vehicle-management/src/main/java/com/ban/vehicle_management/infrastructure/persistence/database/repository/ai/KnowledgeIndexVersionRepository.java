package com.ban.vehicle_management.infrastructure.persistence.database.repository.ai;

import com.ban.vehicle_management.infrastructure.persistence.database.entity.ai.KnowledgeIndexVersionEntity;
import com.ban.vehicle_management.shared.enumeration.ai.KnowledgeIndexVersionStatus;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.time.Instant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface KnowledgeIndexVersionRepository extends JpaRepository<KnowledgeIndexVersionEntity, UUID> {

    List<KnowledgeIndexVersionEntity> findAllByOrderByCreatedAtDesc();

    List<KnowledgeIndexVersionEntity> findByStatus(KnowledgeIndexVersionStatus status);

    Optional<KnowledgeIndexVersionEntity> findByStatusAndActivatedAtIsNotNull(KnowledgeIndexVersionStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select version from KnowledgeIndexVersionEntity version where version.status = :status and version.activatedAt is not null")
    Optional<KnowledgeIndexVersionEntity> findActiveForUpdate(@Param("status") KnowledgeIndexVersionStatus status);

    boolean existsByModelConfigurationId(UUID modelConfigurationId);

    long countByStatusIn(List<KnowledgeIndexVersionStatus> statuses);

    Optional<KnowledgeIndexVersionEntity> findFirstByStatusInOrderByCreatedAtAsc(List<KnowledgeIndexVersionStatus> statuses);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select version from KnowledgeIndexVersionEntity version where version.indexVersionId = :indexVersionId")
    Optional<KnowledgeIndexVersionEntity> findByIdForUpdate(@Param("indexVersionId") UUID indexVersionId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
            UPDATE ai.knowledge_index_versions
            SET build_lease_id = :leaseId,
                build_lease_until = :leaseUntil
            WHERE index_version_id = :indexVersionId
              AND status = 'BUILDING'
              AND (build_lease_until IS NULL OR build_lease_until < now())
            """, nativeQuery = true)
    int tryAcquireBuildLease(
            @Param("indexVersionId") UUID indexVersionId,
            @Param("leaseId") UUID leaseId,
            @Param("leaseUntil") Instant leaseUntil
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
            UPDATE ai.knowledge_index_versions
            SET build_lease_id = NULL,
                build_lease_until = NULL
            WHERE index_version_id = :indexVersionId
              AND build_lease_id = :leaseId
            """, nativeQuery = true)
    int releaseBuildLease(
            @Param("indexVersionId") UUID indexVersionId,
            @Param("leaseId") UUID leaseId
    );
}
