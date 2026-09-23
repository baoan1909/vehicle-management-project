package com.ban.vehicle_management.application.ai.port.out;

import com.ban.vehicle_management.domain.ai.model.RetrievalAudit;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface KnowledgeRetrievalAuditPortOut {

    RetrievalAudit save(RetrievalAudit audit);

    Optional<RetrievalAudit> findById(UUID retrievalAuditId);

    void finalizeForAnswer(
            UUID retrievalAuditId,
            UUID runId,
            UUID outputMessageId,
            Set<UUID> selectedChunkIds);
}
