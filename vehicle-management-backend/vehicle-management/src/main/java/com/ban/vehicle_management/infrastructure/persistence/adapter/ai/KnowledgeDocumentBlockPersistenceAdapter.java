package com.ban.vehicle_management.infrastructure.persistence.adapter.ai;

import com.ban.vehicle_management.application.ai.port.out.KnowledgeDocumentBlockPortOut;
import com.ban.vehicle_management.domain.ai.knowledge.model.KnowledgeDocumentBlock;
import com.ban.vehicle_management.infrastructure.mapper.ai.KnowledgeDocumentBlockPersistenceMapper;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.ai.KnowledgeDocumentBlockRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class KnowledgeDocumentBlockPersistenceAdapter implements KnowledgeDocumentBlockPortOut {

    private final KnowledgeDocumentBlockRepository repository;
    private final KnowledgeDocumentBlockPersistenceMapper mapper;

    public KnowledgeDocumentBlockPersistenceAdapter(
            KnowledgeDocumentBlockRepository repository,
            KnowledgeDocumentBlockPersistenceMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    @Transactional
    public void replaceForVersion(UUID documentId, int documentVersion, List<KnowledgeDocumentBlock> blocks) {
        repository.deleteByDocumentIdAndDocVersion(documentId, documentVersion);
        repository.flush();
        if (blocks != null && !blocks.isEmpty()) {
            repository.saveAll(mapper.toEntityList(blocks));
        }
    }

    @Override
    public List<KnowledgeDocumentBlock> findByDocumentIdAndDocVersion(UUID documentId, int documentVersion) {
        return mapper.toDomainList(
                repository.findByDocumentIdAndDocVersionOrderByBlockIndexAsc(documentId, documentVersion));
    }

    @Override
    public long countByDocumentId(UUID documentId) {
        return repository.countByDocumentId(documentId);
    }
}