package com.ban.vehicle_management.infrastructure.persistence.adapter.ai;

import com.ban.vehicle_management.application.ai.port.out.KnowledgeIndexVersionPortOut;
import com.ban.vehicle_management.domain.ai.model.KnowledgeIndexVersion;
import com.ban.vehicle_management.infrastructure.mapper.ai.KnowledgeIndexVersionPersistenceMapper;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.ai.KnowledgeIndexVersionRepository;
import com.ban.vehicle_management.shared.enumeration.ai.KnowledgeIndexVersionStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.time.Instant;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional(readOnly = true)
public class KnowledgeIndexVersionPersistenceAdapter implements KnowledgeIndexVersionPortOut {

    private final KnowledgeIndexVersionRepository repository;
    private final KnowledgeIndexVersionPersistenceMapper mapper;

    public KnowledgeIndexVersionPersistenceAdapter(
            KnowledgeIndexVersionRepository repository,
            KnowledgeIndexVersionPersistenceMapper mapper
    ) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public List<KnowledgeIndexVersion> findAll() {
        return repository.findAllByOrderByCreatedAtDesc().stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public Optional<KnowledgeIndexVersion> findById(UUID indexVersionId) {
        return repository.findById(indexVersionId).map(mapper::toDomain);
    }

    @Override
    @Transactional
    public Optional<KnowledgeIndexVersion> findByIdForUpdate(UUID indexVersionId) {
        return repository.findByIdForUpdate(indexVersionId).map(mapper::toDomain);
    }

    @Override
    public Optional<KnowledgeIndexVersion> findActive() {
        return repository.findByStatusAndActivatedAtIsNotNull(KnowledgeIndexVersionStatus.ACTIVE)
                .map(mapper::toDomain);
    }

    @Override
    @Transactional
    public Optional<KnowledgeIndexVersion> findActiveForUpdate() {
        return repository.findActiveForUpdate(KnowledgeIndexVersionStatus.ACTIVE).map(mapper::toDomain);
    }

    @Override
    public List<KnowledgeIndexVersion> findByStatus(KnowledgeIndexVersionStatus status) {
        return repository.findByStatus(status).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    @Transactional
    public boolean tryAcquireBuildLease(UUID indexVersionId, UUID leaseId, Instant leaseUntil) {
        return repository.tryAcquireBuildLease(indexVersionId, leaseId, leaseUntil) == 1;
    }

    @Override
    @Transactional
    public void releaseBuildLease(UUID indexVersionId, UUID leaseId) {
        repository.releaseBuildLease(indexVersionId, leaseId);
    }

    @Override
    public boolean existsByModelConfigurationId(UUID modelConfigurationId) {
        return repository.existsByModelConfigurationId(modelConfigurationId);
    }

    @Override
    @Transactional
    public KnowledgeIndexVersion save(KnowledgeIndexVersion indexVersion) {
        return mapper.toDomain(repository.saveAndFlush(mapper.toEntity(indexVersion)));
    }
}
