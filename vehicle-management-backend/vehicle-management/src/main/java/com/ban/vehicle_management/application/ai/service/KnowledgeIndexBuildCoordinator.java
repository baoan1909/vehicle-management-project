package com.ban.vehicle_management.application.ai.service;

import com.ban.vehicle_management.application.ai.port.out.AiModelConfigurationPortOut;
import com.ban.vehicle_management.application.ai.port.out.KnowledgeChunkPortOut;
import com.ban.vehicle_management.application.ai.port.out.KnowledgeIndexVersionPortOut;
import com.ban.vehicle_management.domain.ai.model.AiModelConfiguration;
import com.ban.vehicle_management.domain.ai.model.KnowledgeIndexVersion;
import com.ban.vehicle_management.shared.enumeration.ai.AiModelStatus;
import com.ban.vehicle_management.shared.enumeration.ai.AiUseCase;
import com.ban.vehicle_management.shared.enumeration.ai.KnowledgeIndexVersionStatus;
import java.time.Instant;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Orchestrates the automatic index candidate that follows document approval/archival.
 *
 * Rules (single writer, serialized by an advisory lock):
 * - At most one candidate may exist in DRAFT/BUILDING at a time; when one is already
 *   pending, no new candidate is created.
 * - The candidate snapshots the eligible corpus checksum and expected chunk count at
 *   creation time; the existing build service fails it with KNOWLEDGE_CORPUS_CHANGED
 *   when the corpus moves away before the build finishes.
 * - Only AI_KNOWLEDGE_APPROVE_ALL can activate a candidate; nothing auto-activates.
 */
@Component
public class KnowledgeIndexBuildCoordinator {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeIndexBuildCoordinator.class);

    private final KnowledgeIndexVersionPortOut indexVersionPortOut;
    private final AiModelConfigurationPortOut configurationPortOut;
    private final KnowledgeChunkPortOut chunkPortOut;
    private final EmbeddingPromptFormatter promptFormatter;

    public KnowledgeIndexBuildCoordinator(
            KnowledgeIndexVersionPortOut indexVersionPortOut,
            AiModelConfigurationPortOut configurationPortOut,
            KnowledgeChunkPortOut chunkPortOut,
            EmbeddingPromptFormatter promptFormatter) {
        this.indexVersionPortOut = indexVersionPortOut;
        this.configurationPortOut = configurationPortOut;
        this.chunkPortOut = chunkPortOut;
        this.promptFormatter = promptFormatter;
    }

    /** Requested after a document is approved: creates a candidate when eligible. */
    @Transactional
    public void ensureAutoCandidate() {
        indexVersionPortOut.lockCandidateAdvisory();
        KnowledgeIndexVersion pending = indexVersionPortOut.findFirstPendingCandidate().orElse(null);
        if (pending != null) {
            if (isCorpusStale(pending)) {
                pending.fail("KNOWLEDGE_CORPUS_CHANGED", Instant.now());
                indexVersionPortOut.save(pending);
                log.info("Stale knowledge index candidate {} failed because corpus changed: {}",
                        pending.getIndexVersionId(), pending.getVersionCode());
            } else {
                return;
            }
        }
        AiModelConfiguration configuration = configurationPortOut.findByUseCaseAndStatus(
                        AiUseCase.EMBEDDING, AiModelStatus.ACTIVE)
                .stream()
                .findFirst()
                .orElse(null);
        if (configuration == null || configuration.getOutputDimension() == null) {
            log.warn("Skipping auto knowledge index candidate: no active embedding model configuration with an output dimension");
            return;
        }
        long eligibleChunks = chunkPortOut.countEligibleChunks();
        if (eligibleChunks <= 0) {
            log.debug("Skipping auto knowledge index candidate: no eligible chunks in the corpus");
            return;
        }
        Instant now = Instant.now();
        KnowledgeIndexVersion candidate = KnowledgeIndexVersion.draft(
                autoCode(configuration.getModelId()),
                configuration.getConfigurationId(),
                configuration.getProvider(),
                configuration.getModelId(),
                configuration.getOutputDimension(),
                KnowledgeIndexVersionUseCaseImpl.CHUNKER_VERSION,
                promptFormatter.version(),
                KnowledgeIndexVersionUseCaseImpl.DISTANCE_METRIC,
                KnowledgeIndexVersionUseCaseImpl.NORMALIZATION,
                chunkPortOut.calculateEligibleCorpusChecksum(),
                null,
                now);
        candidate.setExpectedChunkCount(capCount(eligibleChunks));
        candidate.startBuild(null, now);
        indexVersionPortOut.save(candidate);
        log.info("Auto knowledge index candidate created: {} ({})",
                candidate.getIndexVersionId(), candidate.getVersionCode());
    }

    /** Requested after archival or re-upload: stale candidates must die, then rebuild. */
    @Transactional
    public void onCorpusChanged() {
        indexVersionPortOut.lockCandidateAdvisory();
        KnowledgeIndexVersion pending = indexVersionPortOut.findFirstPendingCandidate().orElse(null);
        if (pending != null) {
            pending.fail("KNOWLEDGE_CORPUS_CHANGED", Instant.now());
            indexVersionPortOut.save(pending);
        }
        ensureAutoCandidate();
    }

    private static String autoCode(String modelId) {
        String prefix = modelId == null ? "auto" : modelId.replaceAll("[^A-Za-z0-9_-]", "-");
        String timestamp = Instant.now().toString().replace(':', '-');
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        return "auto-" + prefix + "-" + timestamp + "-" + suffix;
    }

    private boolean isCorpusStale(KnowledgeIndexVersion candidate) {
        String currentChecksum = chunkPortOut.calculateEligibleCorpusChecksum();
        return currentChecksum != null && !currentChecksum.equals(candidate.getContentChecksum());
    }

    private static int capCount(long value) {
        return (int) Math.min(value, Integer.MAX_VALUE);
    }
}
