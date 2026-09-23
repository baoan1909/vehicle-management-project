package com.ban.vehicle_management.infrastructure.persistence.adapter.ai;

import com.ban.vehicle_management.application.ai.port.out.KnowledgeSourcePortOut;
import com.ban.vehicle_management.application.ai.query.KnowledgeSourceQuery;
import com.ban.vehicle_management.domain.ai.knowledge.model.KnowledgeSource;
import com.ban.vehicle_management.infrastructure.mapper.ai.KnowledgeSourcePersistenceMapper;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.ai.KnowledgeSourceRepository;
import com.ban.vehicle_management.infrastructure.persistence.specification.ai.KnowledgeSourceSpecifications;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Component
public class KnowledgeSourcePersistenceAdapter implements KnowledgeSourcePortOut {

    private final KnowledgeSourceRepository repository;
    private final KnowledgeSourcePersistenceMapper mapper;

    public KnowledgeSourcePersistenceAdapter(
            KnowledgeSourceRepository repository,
            KnowledgeSourcePersistenceMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    @Transactional
    public KnowledgeSource save(KnowledgeSource source) {
        return mapper.toDomain(repository.saveAndFlush(mapper.toEntity(source)));
    }

    @Override
    public Optional<KnowledgeSource> findById(UUID sourceId) {
        return repository.findById(sourceId).map(mapper::toDomain);
    }

    @Override
    public Optional<KnowledgeSource> findByIdForUpdate(UUID sourceId) {
        return repository.findById(sourceId).map(mapper::toDomain);
    }

    @Override
    public List<KnowledgeSource> findAll() {
        return repository.findAll().stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public Page<KnowledgeSource> findAll(KnowledgeSourceQuery query, Pageable pageable) {
        KnowledgeSourceSpecifications.Filter filter = new KnowledgeSourceSpecifications.Filter(
                query.status(), query.accessScope(), query.tenantId(), query.keyword());
        return repository.findAll(filter.toSpecification(), pageable).map(mapper::toDomain);
    }

    @Override
    public boolean existsByTitle(String title) {
        return repository.existsByTitleIgnoreCase(title);
    }
}
