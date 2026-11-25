package com.ban.vehicle_management.infrastructure.persistence.adapter.ai;

import com.ban.vehicle_management.application.ai.port.out.CommonKnowledgePortOut;
import com.ban.vehicle_management.domain.ai.model.CommonKnowledgeCandidate;
import com.ban.vehicle_management.infrastructure.mapper.ai.CommonKnowledgePersistenceMapper;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.ai.KnowledgeChunkRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class CommonKnowledgePersistenceAdapter implements CommonKnowledgePortOut {
    private final KnowledgeChunkRepository repository;
    private final CommonKnowledgePersistenceMapper mapper;
    public CommonKnowledgePersistenceAdapter(KnowledgeChunkRepository repository, CommonKnowledgePersistenceMapper mapper) {
        this.repository = repository; this.mapper = mapper;
    }
    @Override public List<CommonKnowledgeCandidate> searchReviewedFaqs(UUID tenantId, List<String> scopes, String query, int limit) {
        if (scopes.isEmpty() || query.isBlank()) return List.of();
        return repository.commonFaqRows(tenantId, scopes, query, Math.min(65, Math.max(1, limit))).stream().map(mapper::toDomain).toList();
    }
    @Override @Transactional
    public List<CommonKnowledgeCandidate> lockReviewedFaqs(UUID tenantId, List<String> scopes, List<UUID> chunkIds) {
        if (scopes.isEmpty() || chunkIds.isEmpty()) return List.of();
        return repository.lockCommonFaqRows(tenantId, scopes, chunkIds).stream().map(mapper::toDomain).toList();
    }
}
