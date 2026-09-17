package com.ban.vehicle_management.application.ai.port.out;

import com.ban.vehicle_management.domain.ai.model.EmbeddingVector;
import com.ban.vehicle_management.domain.ai.model.KnowledgeSearchResult;
import java.util.List;
import java.util.UUID;

public interface KnowledgeRetrievalPortOut {

    List<KnowledgeSearchResult> search(UUID tenantId, String query, List<String> accessScopes, int limit);

    List<KnowledgeSearchResult> searchVector(
            UUID tenantId,
            EmbeddingVector queryVector,
            List<String> accessScopes,
            UUID indexVersionId,
            int limit
    );
}
