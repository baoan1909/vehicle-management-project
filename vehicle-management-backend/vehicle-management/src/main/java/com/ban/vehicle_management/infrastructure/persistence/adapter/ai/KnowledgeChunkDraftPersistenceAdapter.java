package com.ban.vehicle_management.infrastructure.persistence.adapter.ai;

import com.ban.vehicle_management.application.ai.port.out.KnowledgeChunkDraftPortOut;
import com.ban.vehicle_management.domain.ai.knowledge.model.KnowledgeChunkDraft;
import com.ban.vehicle_management.domain.ai.knowledge.model.KnowledgeChunkSnapshot;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.ai.KnowledgeChunkRepository;
import com.ban.vehicle_management.shared.enumeration.ai.KnowledgeChunkStatus;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class KnowledgeChunkDraftPersistenceAdapter implements KnowledgeChunkDraftPortOut {

    private final KnowledgeChunkRepository repository;

    public KnowledgeChunkDraftPersistenceAdapter(KnowledgeChunkRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public void replaceChunksForDocument(
            UUID documentId,
            int documentVersion,
            String chunkerVersion,
            List<KnowledgeChunkDraft> chunks) {
        repository.replaceChunksForDocument(documentId, documentVersion, chunkerVersion, chunks);
    }

    @Override
    public List<KnowledgeChunkSnapshot> findByDocumentIdAndDocVersion(UUID documentId, int documentVersion) {
        return repository.findByDocumentIdAndDocVersion(documentId, documentVersion);
    }

    @Override
    @Transactional
    public void updateStatusByDocumentId(UUID documentId, KnowledgeChunkStatus from, KnowledgeChunkStatus to) {
        repository.updateStatusByDocumentId(documentId, from.name(), to.name());
    }

    @Override
    public long countByDocumentId(UUID documentId) {
        return repository.countByDocumentId(documentId);
    }
}