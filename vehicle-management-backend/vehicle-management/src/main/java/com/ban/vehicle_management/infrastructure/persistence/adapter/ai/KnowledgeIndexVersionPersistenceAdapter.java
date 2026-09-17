package com.ban.vehicle_management.infrastructure.persistence.adapter.ai;

import com.ban.vehicle_management.application.ai.port.out.KnowledgeIndexVersionPortOut;
import com.ban.vehicle_management.domain.ai.model.KnowledgeIndexVersion;
import com.ban.vehicle_management.infrastructure.mapper.ai.KnowledgeIndexVersionPersistenceMapper;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.ai.KnowledgeIndexVersionRepository;
import com.ban.vehicle_management.shared.enumeration.ai.KnowledgeIndexVersionStatus;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.time.Instant;
import javax.sql.DataSource;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional(readOnly = true)
public class KnowledgeIndexVersionPersistenceAdapter implements KnowledgeIndexVersionPortOut {

    private static final String CANDIDATE_ADVISORY_LOCK_KEY = "ai_knowledge_auto_candidate";

    private final KnowledgeIndexVersionRepository repository;
    private final KnowledgeIndexVersionPersistenceMapper mapper;
    private final DataSource dataSource;

    public KnowledgeIndexVersionPersistenceAdapter(
            KnowledgeIndexVersionRepository repository,
            KnowledgeIndexVersionPersistenceMapper mapper,
            DataSource dataSource
    ) {
        this.repository = repository;
        this.mapper = mapper;
        this.dataSource = dataSource;
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
    public Optional<KnowledgeIndexVersion> findFirstPendingCandidate() {
        return repository.findFirstByStatusInOrderByCreatedAtAsc(
                        List.of(KnowledgeIndexVersionStatus.DRAFT, KnowledgeIndexVersionStatus.BUILDING))
                .map(mapper::toDomain);
    }

    @Override
    public long countPendingCandidates() {
        return repository.countByStatusIn(
                List.of(KnowledgeIndexVersionStatus.DRAFT, KnowledgeIndexVersionStatus.BUILDING));
    }

    @Override
    @Transactional
    public void lockCandidateAdvisory() {
        // Use DataSourceUtils.getConnection to get a connection bound to the current Spring transaction.
        // This ensures the advisory lock is held until the transaction commits/rollbacks.
        Connection connection = DataSourceUtils.getConnection(dataSource);
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT pg_advisory_xact_lock(hashtext(?))")) {
            statement.setString(1, CANDIDATE_ADVISORY_LOCK_KEY);
            statement.execute();
        } catch (SQLException exception) {
            throw new IllegalStateException("Cannot acquire knowledge candidate advisory lock", exception);
        }
        // Do NOT close the connection - it's managed by Spring's transaction synchronization.
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
