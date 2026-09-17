package com.ban.vehicle_management.application.ai.service;

import com.ban.vehicle_management.application.ai.port.out.AiModelConfigurationPortOut;
import com.ban.vehicle_management.application.ai.port.out.KnowledgeIndexVersionPortOut;
import com.ban.vehicle_management.application.ai.port.out.KnowledgeRetrievalPortOut;
import com.ban.vehicle_management.domain.ai.model.AiModelConfiguration;
import com.ban.vehicle_management.domain.ai.model.EmbeddingRequest;
import com.ban.vehicle_management.domain.ai.model.EmbeddingResult;
import com.ban.vehicle_management.domain.ai.model.KnowledgeIndexVersion;
import com.ban.vehicle_management.domain.ai.model.KnowledgeSearchResult;
import com.ban.vehicle_management.shared.enumeration.ai.AiUseCase;
import com.ban.vehicle_management.shared.enumeration.ai.AiModelStatus;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Knowledge retrieval entry point used by the assistant RAG tool. Embedding search runs
 * against the ACTIVE index version only; any failure or missing setup falls back to the
 * existing lexical (FTS) search behind a feature flag.
 */
@Service
public class KnowledgeRetrievalService {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeRetrievalService.class);

    private final KnowledgeRetrievalPortOut retrievalPortOut;
    private final KnowledgeIndexVersionPortOut indexVersionPortOut;
    private final AiModelConfigurationPortOut configurationPortOut;
    private final EmbeddingService embeddingService;
    private final EmbeddingPromptFormatter promptFormatter;
    private final PiiRedactionService piiRedactionService;
    private final EmbeddingProperties properties;

    public KnowledgeRetrievalService(
            KnowledgeRetrievalPortOut retrievalPortOut,
            KnowledgeIndexVersionPortOut indexVersionPortOut,
            AiModelConfigurationPortOut configurationPortOut,
            EmbeddingService embeddingService,
            EmbeddingPromptFormatter promptFormatter,
            PiiRedactionService piiRedactionService,
            EmbeddingProperties properties
    ) {
        this.retrievalPortOut = retrievalPortOut;
        this.indexVersionPortOut = indexVersionPortOut;
        this.configurationPortOut = configurationPortOut;
        this.embeddingService = embeddingService;
        this.promptFormatter = promptFormatter;
        this.piiRedactionService = piiRedactionService;
        this.properties = properties;
    }

    public List<KnowledgeSearchResult> searchKnowledge(UUID tenantId, String query, List<String> accessScopes, int limit) {
        if (query == null || query.isBlank() || !properties.isEnabled() || !properties.isVectorSearchEnabled()) {
            return lexicalSearch(tenantId, query, accessScopes, limit);
        }
        KnowledgeIndexVersion active = indexVersionPortOut.findActive().orElse(null);
        if (active == null) {
            return lexicalSearch(tenantId, query, accessScopes, limit);
        }
        AiModelConfiguration configuration = configurationPortOut.findById(active.getModelConfigurationId()).orElse(null);
        if (configuration == null || configuration.getUseCase() != AiUseCase.EMBEDDING
                || configuration.getOutputDimension() == null
                || configuration.getOutputDimension() != active.getDimension()
                || configuration.getStatus() == AiModelStatus.DISABLED
                || configuration.getProvider() != active.getProvider()
                || !configuration.getModelId().equals(active.getModelId())
                || !promptFormatter.supports(active.getEmbeddingPromptVersion())) {
            return lexicalSearch(tenantId, query, accessScopes, limit);
        }
        try {
            String redactedQuery = piiRedactionService.redact(query).value();
            String formatted = promptFormatter.formatQuery(active.getEmbeddingPromptVersion(), redactedQuery);
            EmbeddingResult result = embeddingService.embed(new EmbeddingRequest(formatted, null), configuration);
            if (!result.isSuccess() || result.getVector().dimension() != active.getDimension()) {
                return lexicalSearch(tenantId, query, accessScopes, limit);
            }
            List<KnowledgeSearchResult> vectorResults = retrievalPortOut.searchVector(
                    tenantId,
                    result.getVector(),
                    accessScopes,
                    active.getIndexVersionId(),
                    limit
            );
            if (vectorResults.isEmpty()) {
                return lexicalSearch(tenantId, query, accessScopes, limit);
            }
            if (!properties.isLexicalFallbackEnabled()) {
                return vectorResults;
            }
            return merge(vectorResults, lexicalSearch(tenantId, query, accessScopes, limit), limit);
        } catch (Exception exception) {
            log.warn("Vector search failed, falling back to lexical search", exception);
            return lexicalSearch(tenantId, query, accessScopes, limit);
        }
    }

    private List<KnowledgeSearchResult> lexicalSearch(UUID tenantId, String query, List<String> accessScopes, int limit) {
        return retrievalPortOut.search(tenantId, query, accessScopes, limit);
    }

    private List<KnowledgeSearchResult> merge(
            List<KnowledgeSearchResult> vectorResults,
            List<KnowledgeSearchResult> lexicalResults,
            int limit
    ) {
        Map<UUID, KnowledgeSearchResult> items = new LinkedHashMap<>();
        Map<UUID, Double> scores = new HashMap<>();
        addReciprocalRankScores(vectorResults, items, scores);
        addReciprocalRankScores(lexicalResults, items, scores);

        return items.values().stream()
                .sorted(Comparator.comparingDouble(
                        (KnowledgeSearchResult item) -> scores.getOrDefault(item.chunkId(), 0D)
                ).reversed())
                .limit(limit)
                .map(item -> new KnowledgeSearchResult(
                        item.documentId(),
                        item.chunkId(),
                        item.title(),
                        item.content(),
                        item.summary(),
                        item.sourcePage(),
                        item.sourceSection(),
                        BigDecimal.valueOf(scores.getOrDefault(item.chunkId(), 0D))
                                .setScale(8, RoundingMode.HALF_UP)
                ))
                .toList();
    }

    private void addReciprocalRankScores(
            List<KnowledgeSearchResult> rankedResults,
            Map<UUID, KnowledgeSearchResult> items,
            Map<UUID, Double> scores
    ) {
        final int rankConstant = 60;
        for (int index = 0; index < rankedResults.size(); index++) {
            KnowledgeSearchResult item = rankedResults.get(index);
            items.putIfAbsent(item.chunkId(), item);
            scores.merge(item.chunkId(), 1D / (rankConstant + index + 1D), Double::sum);
        }
    }
}
