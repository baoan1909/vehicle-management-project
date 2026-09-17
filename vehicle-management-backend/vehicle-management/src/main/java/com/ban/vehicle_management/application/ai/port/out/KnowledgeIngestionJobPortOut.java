package com.ban.vehicle_management.application.ai.port.out;

import com.ban.vehicle_management.application.ai.query.KnowledgeIngestionJobQuery;
import com.ban.vehicle_management.domain.ai.knowledge.model.KnowledgeIngestionJob;
import com.ban.vehicle_management.domain.ai.knowledge.model.KnowledgeIngestionJobEvent;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Worker-friendly ingestion job store. Claiming uses a short transaction with
 * FOR UPDATE SKIP LOCKED; heartbeats renew the lease without releasing it.
 */
public interface KnowledgeIngestionJobPortOut {

    KnowledgeIngestionJob save(KnowledgeIngestionJob job);

    Optional<KnowledgeIngestionJob> findById(UUID ingestionJobId);

    Optional<KnowledgeIngestionJob> findByIdForUpdate(UUID ingestionJobId);

    Optional<KnowledgeIngestionJob> findByDocumentId(UUID documentId);

    Optional<KnowledgeIngestionJob> findLatestByDocumentId(UUID documentId);

    Optional<KnowledgeIngestionJob> findByRequestedByAndIdempotencyKey(UUID requestedBy, String idempotencyKey);

    List<KnowledgeIngestionJob> findOpenByDocumentId(UUID documentId);

    List<KnowledgeIngestionJob> findAll();

    Page<KnowledgeIngestionJob> findAll(KnowledgeIngestionJobQuery query, Pageable pageable);

    List<KnowledgeIngestionJob> findByStatus(String status);

    /**
     * Atomically claims due jobs for a worker. Updating happens inside the same
     * transaction as the SELECT FOR UPDATE so no job is processed twice.
     */
    List<KnowledgeIngestionJob> claimDueJobs(
            Instant now,
            Instant lockExpiresAt,
            String workerId,
            int limit,
            Duration retryInitialDelay);

    boolean renewLease(UUID ingestionJobId, String workerId, Instant lockExpiresAt);

    void appendEvent(KnowledgeIngestionJobEvent event);

    List<KnowledgeIngestionJobEvent> findEventsByJobId(UUID ingestionJobId);
}
