package com.ban.vehicle_management.application.ai.port.out;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface KnowledgeQualityMetricsPortOut {

    KnowledgeQualityMetrics snapshot(Instant now, Instant stuckJobCutoff, Instant staleCandidateCutoff);

    record KnowledgeQualityMetrics(
            Map<String, Long> sourcesByStatus,
            Map<String, Long> documentsByStatus,
            Map<String, Long> jobsByStatus,
            Map<String, Long> indexVersionsByStatus,
            long activeIndexCount,
            long totalIndexVersionCount,
            long stuckJobCount,
            long staleCandidateCount,
            long expiredDocumentCount,
            long missingEmbeddingCount,
            long incompleteActiveIndexCount,
            List<UUID> activeModelConfigurationIds) {
    }
}
