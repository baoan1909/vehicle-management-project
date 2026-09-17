package com.ban.vehicle_management.infrastructure.persistence.adapter.ai;

import com.ban.vehicle_management.application.ai.port.out.KnowledgeIngestionJobPortOut;
import com.ban.vehicle_management.application.ai.query.KnowledgeIngestionJobQuery;
import com.ban.vehicle_management.domain.ai.knowledge.model.KnowledgeIngestionJob;
import com.ban.vehicle_management.domain.ai.knowledge.model.KnowledgeIngestionJobEvent;
import com.ban.vehicle_management.infrastructure.mapper.ai.KnowledgeIngestionJobEventPersistenceMapper;
import com.ban.vehicle_management.infrastructure.mapper.ai.KnowledgeIngestionJobPersistenceMapper;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.ai.KnowledgeIngestionJobEventRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.ai.KnowledgeIngestionJobRepository;
import com.ban.vehicle_management.infrastructure.persistence.specification.ai.KnowledgeIngestionJobSpecifications;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Component
public class KnowledgeIngestionJobPersistenceAdapter implements KnowledgeIngestionJobPortOut {

    private final KnowledgeIngestionJobRepository repository;
    private final KnowledgeIngestionJobPersistenceMapper mapper;
    private final KnowledgeIngestionJobEventRepository eventRepository;
    private final KnowledgeIngestionJobEventPersistenceMapper eventMapper;

    public KnowledgeIngestionJobPersistenceAdapter(
            KnowledgeIngestionJobRepository repository,
            KnowledgeIngestionJobPersistenceMapper mapper,
            KnowledgeIngestionJobEventRepository eventRepository,
            KnowledgeIngestionJobEventPersistenceMapper eventMapper) {
        this.repository = repository;
        this.mapper = mapper;
        this.eventRepository = eventRepository;
        this.eventMapper = eventMapper;
    }

    @Override
    @Transactional
    public KnowledgeIngestionJob save(KnowledgeIngestionJob job) {
        return mapper.toDomain(repository.saveAndFlush(mapper.toEntity(job)));
    }

    @Override
    public Optional<KnowledgeIngestionJob> findById(UUID ingestionJobId) {
        return repository.findById(ingestionJobId).map(mapper::toDomain);
    }

    @Override
    @Transactional
    public Optional<KnowledgeIngestionJob> findByIdForUpdate(UUID ingestionJobId) {
        return repository.findByIdForUpdate(ingestionJobId).map(mapper::toDomain);
    }

    @Override
    public Optional<KnowledgeIngestionJob> findByDocumentId(UUID documentId) {
        return repository.findByDocumentId(documentId).map(mapper::toDomain);
    }

    @Override
    public Optional<KnowledgeIngestionJob> findLatestByDocumentId(UUID documentId) {
        return repository.findFirstByDocumentIdOrderByCreatedAtDesc(documentId).map(mapper::toDomain);
    }

    @Override
    public Optional<KnowledgeIngestionJob> findByRequestedByAndIdempotencyKey(
            UUID requestedBy,
            String idempotencyKey) {
        return repository.findByRequestedByAndIdempotencyKey(requestedBy, idempotencyKey).map(mapper::toDomain);
    }

    @Override
    public List<KnowledgeIngestionJob> findOpenByDocumentId(UUID documentId) {
        List<String> openStatuses = List.of("PENDING", "PROCESSING", "RETRYING", "REVIEW");
        return repository.findByDocumentIdAndStatusInOrderByCreatedAtDesc(documentId, openStatuses).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<KnowledgeIngestionJob> findAll() {
        return repository.findAll().stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public Page<KnowledgeIngestionJob> findAll(KnowledgeIngestionJobQuery query, Pageable pageable) {
        KnowledgeIngestionJobSpecifications.Filter filter = new KnowledgeIngestionJobSpecifications.Filter(
                query.documentId(), query.status(), query.currentStage(), query.errorCode(),
                query.createdFrom(), query.createdTo(), query.keyword());
        return repository.findAll(filter.toSpecification(), pageable).map(mapper::toDomain);
    }

    @Override
    public List<KnowledgeIngestionJob> findByStatus(String status) {
        return repository.findByStatusOrderByCreatedAtAsc(status).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    @Transactional
    public List<KnowledgeIngestionJob> claimDueJobs(
            Instant now,
            Instant lockExpiresAt,
            String workerId,
            int limit,
            Duration retryInitialDelay) {
        return repository.claimDueJobs(now, lockExpiresAt, workerId, Math.max(1, limit))
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    @Transactional
    public boolean renewLease(UUID ingestionJobId, String workerId, Instant lockExpiresAt) {
        return repository.renewLease(ingestionJobId, workerId, lockExpiresAt, Instant.now()) == 1;
    }

    @Override
    @Transactional
    public void appendEvent(KnowledgeIngestionJobEvent event) {
        eventRepository.save(eventMapper.toEntity(event));
    }

    @Override
    public List<KnowledgeIngestionJobEvent> findEventsByJobId(UUID ingestionJobId) {
        return eventMapper.toDomainList(
                eventRepository.findByIngestionJobIdOrderByCreatedAtAsc(ingestionJobId));
    }
}
