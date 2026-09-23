package com.ban.vehicle_management.infrastructure.persistence.adapter.ai;

import com.ban.vehicle_management.application.ai.port.out.KnowledgeQualityMetricsPortOut;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.ai.KnowledgeQualityMetricsRepository;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class KnowledgeQualityMetricsPersistenceAdapter implements KnowledgeQualityMetricsPortOut {

    private final KnowledgeQualityMetricsRepository repository;

    public KnowledgeQualityMetricsPersistenceAdapter(KnowledgeQualityMetricsRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(readOnly = true)
    public KnowledgeQualityMetrics snapshot(
            Instant now,
            Instant stuckJobCutoff,
            Instant staleCandidateCutoff) {
        Map<String, Long> sourceCounts = toMap(repository.countSourcesByStatus());
        Map<String, Long> documentCounts = toMap(repository.countDocumentsByStatus());
        Map<String, Long> jobCounts = toMap(repository.countJobsByStatus());
        Map<String, Long> indexCounts = toMap(repository.countIndexVersionsByStatus());
        return new KnowledgeQualityMetrics(
                sourceCounts,
                documentCounts,
                jobCounts,
                indexCounts,
                indexCounts.getOrDefault("ACTIVE", 0L),
                indexCounts.values().stream().mapToLong(Long::longValue).sum(),
                repository.countStuckJobs(stuckJobCutoff),
                repository.countStaleCandidates(staleCandidateCutoff),
                repository.countExpiredDocuments(now),
                repository.countMissingEmbeddings(),
                repository.countIncompleteActiveIndexes(),
                repository.findActiveModelConfigurationIds());
    }

    private Map<String, Long> toMap(List<KnowledgeQualityMetricsRepository.StatusCountView> rows) {
        Map<String, Long> result = new LinkedHashMap<>();
        for (KnowledgeQualityMetricsRepository.StatusCountView row : rows) {
            result.put(row.getStatus() == null ? "UNKNOWN" : row.getStatus(), row.getTotal());
        }
        return result;
    }
}
