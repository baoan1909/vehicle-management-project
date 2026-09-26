package com.ban.vehicle_management.application.ai.service;

import com.ban.vehicle_management.application.ai.port.in.KnowledgeQualityPortIn;
import com.ban.vehicle_management.application.ai.port.out.AiModelConfigurationPortOut;
import com.ban.vehicle_management.application.ai.port.out.KnowledgeQualityMetricsPortOut;
import com.ban.vehicle_management.application.ai.port.out.KnowledgeQualityMetricsPortOut.KnowledgeQualityMetrics;
import com.ban.vehicle_management.shared.enumeration.ai.AiModelStatus;
import com.ban.vehicle_management.shared.enumeration.ai.AiUseCase;
import java.time.Duration;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;

@Service
public class KnowledgeQualityService implements KnowledgeQualityPortIn {

    private final KnowledgeQualityMetricsPortOut metricsPortOut;
    private final AiModelConfigurationPortOut configurationPortOut;
    private final Duration jobStuckTolerance;
    private final Duration candidateStaleTolerance;
    private final Clock clock;

    @Autowired
    public KnowledgeQualityService(
            KnowledgeQualityMetricsPortOut metricsPortOut,
            AiModelConfigurationPortOut configurationPortOut,
            AiQualityProperties properties,
            Clock clock) {
        this(metricsPortOut, configurationPortOut, properties.getJobStuckTolerance(),
                properties.getCandidateStaleTolerance(), clock);
    }

    public KnowledgeQualityService(
            KnowledgeQualityMetricsPortOut metricsPortOut,
            AiModelConfigurationPortOut configurationPortOut) {
        this(metricsPortOut, configurationPortOut, Duration.ofMinutes(30),
                Duration.ofMinutes(60), Clock.systemUTC());
    }

    private KnowledgeQualityService(
            KnowledgeQualityMetricsPortOut metricsPortOut,
            AiModelConfigurationPortOut configurationPortOut,
            Duration jobStuckTolerance,
            Duration candidateStaleTolerance,
            Clock clock) {
        this.metricsPortOut = metricsPortOut;
        this.configurationPortOut = configurationPortOut;
        this.jobStuckTolerance = jobStuckTolerance;
        this.candidateStaleTolerance = candidateStaleTolerance;
        this.clock = clock;
    }

    public KnowledgeQualityDashboard summarize() {
        Instant now = Instant.now(clock);
        KnowledgeQualityMetrics metrics = metricsPortOut.snapshot(
                now,
                now.minus(jobStuckTolerance),
                now.minus(candidateStaleTolerance));
        List<KnowledgeQualityWarning> warnings = buildWarnings(metrics);
        return new KnowledgeQualityDashboard(
                metrics.sourcesByStatus(),
                metrics.documentsByStatus(),
                metrics.jobsByStatus(),
                metrics.indexVersionsByStatus(),
                metrics.activeIndexCount(),
                metrics.totalIndexVersionCount(),
                warnings);
    }

    private List<KnowledgeQualityWarning> buildWarnings(KnowledgeQualityMetrics metrics) {
        List<KnowledgeQualityWarning> warnings = new ArrayList<>();
        if (metrics.activeIndexCount() == 0) {
            warnings.add(new KnowledgeQualityWarning(
                    "NO_ACTIVE_INDEX", "WARNING", 0L,
                    "Chưa có phiên bản chỉ mục đang hoạt động. Hãy xây dựng và kích hoạt một phiên bản."));
        }
        boolean embeddingConfigActive = configurationPortOut
                .findByUseCaseAndStatus(AiUseCase.EMBEDDING, AiModelStatus.ACTIVE)
                .stream()
                .findFirst()
                .isPresent();
        if (!embeddingConfigActive) {
            warnings.add(new KnowledgeQualityWarning(
                    "EMBEDDING_CONFIG_INACTIVE", "DANGER", 0L,
                    "Chưa có cấu hình model embedding đang hoạt động. Tài liệu mới sẽ không thể tạo embedding."));
        }
        long stuckJobs = metrics.stuckJobCount();
        if (stuckJobs > 0) {
            warnings.add(new KnowledgeQualityWarning(
                    "STUCK_INGESTION_JOBS", "WARNING", stuckJobs,
                    stuckJobs + " công việc xử lý đang mở nhưng không có tiến triển trong vòng hơn "
                            + jobStuckTolerance.toMinutes() + " phút."));
        }
        long staleCandidates = metrics.staleCandidateCount();
        if (staleCandidates > 0) {
            warnings.add(new KnowledgeQualityWarning(
                    "STALE_INDEX_CANDIDATES", "WARNING", staleCandidates,
                    staleCandidates + " phiên bản chỉ mục dự thảo/đang xây dựng bị bỏ dở hơn "
                            + candidateStaleTolerance.toMinutes() + " phút."));
        }
        long failedJobs = metrics.jobsByStatus().getOrDefault("FAILED", 0L);
        addWarning(warnings, failedJobs, "FAILED_INGESTION_JOBS", "DANGER",
                " công việc xử lý tài liệu đã thất bại.");

        long expiredDocuments = metrics.expiredDocumentCount();
        addWarning(warnings, expiredDocuments, "EXPIRED_DOCUMENTS", "WARNING",
                " tài liệu đã hết hiệu lực nhưng vẫn ở trạng thái sẵn sàng.");

        long missingEmbeddings = metrics.missingEmbeddingCount();
        addWarning(warnings, missingEmbeddings, "MISSING_EMBEDDINGS", "DANGER",
                " chunk của tài liệu sẵn sàng đang thiếu embedding.");

        long failedIndexes = metrics.indexVersionsByStatus().getOrDefault("FAILED", 0L);
        addWarning(warnings, failedIndexes, "INDEX_BUILD_FAILED", "DANGER",
                " phiên bản chỉ mục xây dựng thất bại.");

        addWarning(warnings, metrics.incompleteActiveIndexCount(), "ACTIVE_INDEX_INCOMPLETE", "DANGER",
                " chỉ mục đang hoạt động chưa có đủ embedding hoặc có chunk thất bại.");
        long unavailableModels = metrics.activeModelConfigurationIds().stream()
                .filter(configurationId -> configurationPortOut.findById(configurationId)
                        .filter(configuration -> configuration.getStatus() == AiModelStatus.ACTIVE)
                        .isEmpty())
                .count();
        addWarning(warnings, unavailableModels, "MODEL_CONFIGURATION_UNAVAILABLE", "DANGER",
                " model embedding của chỉ mục đang hoạt động không còn khả dụng.");
        return warnings;
    }

    private static void addWarning(
            List<KnowledgeQualityWarning> warnings,
            long count,
            String code,
            String severity,
            String suffix) {
        if (count > 0) {
            warnings.add(new KnowledgeQualityWarning(code, severity, count, count + suffix));
        }
    }

    public record KnowledgeQualityWarning(String code, String severity, Long count, String message) {
    }

    public record KnowledgeQualityDashboard(
            Map<String, Long> sourcesByStatus,
            Map<String, Long> documentsByStatus,
            Map<String, Long> jobsByStatus,
            Map<String, Long> indexVersionsByStatus,
            long activeIndexCount,
            long totalIndexVersionCount,
            List<KnowledgeQualityWarning> warnings) {
    }
}
