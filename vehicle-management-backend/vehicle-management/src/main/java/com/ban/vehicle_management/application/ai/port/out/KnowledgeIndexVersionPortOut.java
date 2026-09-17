package com.ban.vehicle_management.application.ai.port.out;

import com.ban.vehicle_management.domain.ai.model.KnowledgeIndexVersion;
import com.ban.vehicle_management.shared.enumeration.ai.KnowledgeIndexVersionStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.time.Instant;

public interface KnowledgeIndexVersionPortOut {

    List<KnowledgeIndexVersion> findAll();

    Optional<KnowledgeIndexVersion> findById(UUID indexVersionId);

    Optional<KnowledgeIndexVersion> findByIdForUpdate(UUID indexVersionId);

    Optional<KnowledgeIndexVersion> findActive();

    Optional<KnowledgeIndexVersion> findActiveForUpdate();

    List<KnowledgeIndexVersion> findByStatus(KnowledgeIndexVersionStatus status);

    Optional<KnowledgeIndexVersion> findFirstPendingCandidate();

    long countPendingCandidates();

    void lockCandidateAdvisory();

    boolean tryAcquireBuildLease(UUID indexVersionId, UUID leaseId, Instant leaseUntil);

    void releaseBuildLease(UUID indexVersionId, UUID leaseId);

    boolean existsByModelConfigurationId(UUID modelConfigurationId);

    KnowledgeIndexVersion save(KnowledgeIndexVersion indexVersion);
}
