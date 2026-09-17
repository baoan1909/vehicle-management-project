package com.ban.vehicle_management.application.ai.service;

import com.ban.vehicle_management.application.ai.port.out.AiModelConfigurationPortOut;
import com.ban.vehicle_management.application.ai.port.out.KnowledgeChunkPortOut;
import com.ban.vehicle_management.application.ai.port.out.KnowledgeEmbeddingPortOut;
import com.ban.vehicle_management.application.ai.port.out.KnowledgeIndexVersionPortOut;
import com.ban.vehicle_management.domain.ai.model.AiModelConfiguration;
import com.ban.vehicle_management.domain.ai.model.EmbeddingRequest;
import com.ban.vehicle_management.domain.ai.model.EmbeddingResult;
import com.ban.vehicle_management.domain.ai.model.EmbeddingVector;
import com.ban.vehicle_management.domain.ai.model.KnowledgeChunk;
import com.ban.vehicle_management.domain.ai.model.KnowledgeIndexVersion;
import com.ban.vehicle_management.shared.enumeration.ai.AiUseCase;
import com.ban.vehicle_management.shared.enumeration.ai.AiModelStatus;
import com.ban.vehicle_management.shared.enumeration.ai.KnowledgeIndexVersionStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Background index build. Each run embeds a bounded batch of eligible chunks and advances
 * the progress counters. Progress saves use the optimistic lock so a slower concurrent
 * build run simply bumps the version and its own save is retried on the next tick.
 */
@Service
public class KnowledgeIndexBuildService {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeIndexBuildService.class);

    private final KnowledgeIndexVersionPortOut indexVersionPortOut;
    private final AiModelConfigurationPortOut configurationPortOut;
    private final KnowledgeChunkPortOut chunkPortOut;
    private final KnowledgeEmbeddingPortOut embeddingPortOut;
    private final EmbeddingService embeddingService;
    private final EmbeddingPromptFormatter promptFormatter;
    private final PiiRedactionService piiRedactionService;
    private final EmbeddingProperties properties;

    public KnowledgeIndexBuildService(
            KnowledgeIndexVersionPortOut indexVersionPortOut,
            AiModelConfigurationPortOut configurationPortOut,
            KnowledgeChunkPortOut chunkPortOut,
            KnowledgeEmbeddingPortOut embeddingPortOut,
            EmbeddingService embeddingService,
            EmbeddingPromptFormatter promptFormatter,
            PiiRedactionService piiRedactionService,
            EmbeddingProperties properties
    ) {
        this.indexVersionPortOut = indexVersionPortOut;
        this.configurationPortOut = configurationPortOut;
        this.chunkPortOut = chunkPortOut;
        this.embeddingPortOut = embeddingPortOut;
        this.embeddingService = embeddingService;
        this.promptFormatter = promptFormatter;
        this.piiRedactionService = piiRedactionService;
        this.properties = properties;
    }

    public void process(UUID indexVersionId) {
        UUID leaseId = UUID.randomUUID();
        if (!indexVersionPortOut.tryAcquireBuildLease(
                indexVersionId,
                leaseId,
                Instant.now().plus(properties.getBuildLeaseDuration())
        )) {
            return;
        }
        try {
            processWithLease(indexVersionId);
        } finally {
            indexVersionPortOut.releaseBuildLease(indexVersionId, leaseId);
        }
    }

    private void processWithLease(UUID indexVersionId) {
        KnowledgeIndexVersion version = indexVersionPortOut.findById(indexVersionId).orElse(null);
        if (version == null || version.getStatus() != KnowledgeIndexVersionStatus.BUILDING) {
            return;
        }
        Instant now = Instant.now();
        AiModelConfiguration configuration = configurationPortOut.findById(version.getModelConfigurationId()).orElse(null);
        String configurationFailure = configurationFailure(version, configuration);
        if (configurationFailure != null) {
            failVersion(version, configurationFailure);
            return;
        }

        List<KnowledgeChunk> pending = chunkPortOut.findEligibleChunks(indexVersionId, properties.getBuildBatchSize());
        if (pending.isEmpty()) {
            finishOrFail(version, now);
            return;
        }
        for (KnowledgeChunk chunk : pending) {
            if (!embedChunk(version, configuration, chunk, now)) {
                return;
            }
        }
        version.setEmbeddedChunkCount(capCount(embeddingPortOut.countEmbeddedByIndexVersion(indexVersionId)));
        version.setUpdatedAt(now);
        indexVersionPortOut.save(version);
    }

    private boolean embedChunk(
            KnowledgeIndexVersion version,
            AiModelConfiguration configuration,
            KnowledgeChunk chunk,
            Instant now
    ) {
        String title = piiRedactionService.redact(chunk.title() == null ? "" : chunk.title()).value();
        String content = piiRedactionService.redact(chunk.content() == null ? "" : chunk.content()).value();
        String documentText = promptFormatter.formatDocument(version.getEmbeddingPromptVersion(), title, content);
        EmbeddingResult result = embeddingService.embed(new EmbeddingRequest(documentText, chunk.chunkId()), configuration);
        if (!result.isSuccess()) {
            String failureCode = result.getFailureCode() == null ? "EMBEDDING_FAILED" : result.getFailureCode();
            log.warn("Knowledge index build failed for version {} on chunk {}: {}",
                    version.getIndexVersionId(), chunk.chunkId(), failureCode);
            failVersion(version, failureCode);
            return false;
        }
        EmbeddingVector vector = result.getVector();
        if (vector.dimension() != configuration.getOutputDimension()) {
            failVersion(version, "EMBEDDING_DIMENSION_MISMATCH");
            return false;
        }
        embeddingPortOut.insertEmbedding(
                chunk.chunkId(),
                version.getIndexVersionId(),
                vector,
                configuration.getModelId(),
                configuration.getOutputDimension(),
                chunk.documentVersion() == null ? 1 : chunk.documentVersion(),
                now
        );
        return true;
    }

    private String configurationFailure(
            KnowledgeIndexVersion version,
            AiModelConfiguration configuration
    ) {
        if (configuration == null
                || configuration.getUseCase() != AiUseCase.EMBEDDING
                || configuration.getOutputDimension() == null
                || configuration.getStatus() == AiModelStatus.DISABLED) {
            return "EMBEDDING_CONFIG_INVALID";
        }
        if (configuration.getProvider() != version.getProvider()
                || !configuration.getModelId().equals(version.getModelId())) {
            return "EMBEDDING_MODEL_SNAPSHOT_MISMATCH";
        }
        if (configuration.getOutputDimension() != version.getDimension()) {
            return "EMBEDDING_DIMENSION_MISMATCH";
        }
        if (!promptFormatter.supports(version.getEmbeddingPromptVersion())) {
            return "EMBEDDING_PROMPT_VERSION_UNSUPPORTED";
        }
        return null;
    }

    private void finishOrFail(KnowledgeIndexVersion version, Instant now) {
        long embedded = embeddingPortOut.countEmbeddedByIndexVersion(version.getIndexVersionId());
        version.setEmbeddedChunkCount(capCount(embedded));
        if (version.getFailedChunkCount() > 0) {
            failVersion(version, version.getFailureCode() == null ? "EMBEDDING_FAILED" : version.getFailureCode());
            return;
        }
        if (embedded < version.getExpectedChunkCount()) {
            failVersion(version, "EMBEDDING_COVERAGE_UNREACHABLE");
            return;
        }
        if (!java.util.Objects.equals(
                version.getContentChecksum(),
                chunkPortOut.calculateEligibleCorpusChecksum()
        )) {
            failVersion(version, "KNOWLEDGE_CORPUS_CHANGED");
            return;
        }
        version.markReady(now);
        indexVersionPortOut.save(version);
    }

    private void failVersion(KnowledgeIndexVersion version, String failureCode) {
        version.setFailedChunkCount(1);
        version.fail(failureCode, Instant.now());
        indexVersionPortOut.save(version);
    }

    private int capCount(long value) {
        return (int) Math.min(value, Integer.MAX_VALUE);
    }
}
