package com.ban.vehicle_management.application.ai.service;

import com.ban.vehicle_management.application.ai.port.in.KnowledgeRetrievalPortIn;
import com.ban.vehicle_management.application.ai.port.out.AiModelConfigurationPortOut;
import com.ban.vehicle_management.application.ai.port.out.KnowledgeIndexVersionPortOut;
import com.ban.vehicle_management.application.ai.port.out.KnowledgeRetrievalPortOut;
import com.ban.vehicle_management.domain.ai.model.AiModelConfiguration;
import com.ban.vehicle_management.domain.ai.model.EmbeddingRequest;
import com.ban.vehicle_management.domain.ai.model.EmbeddingResult;
import com.ban.vehicle_management.domain.ai.model.KnowledgeIndexVersion;
import com.ban.vehicle_management.domain.ai.model.KnowledgeRetrievalResult;
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
 * Knowledge retrieval entry point used by the assistant RAG tool and by the admin
 * retrieval-test screen.
 *
 * Both vector and lexical retrieval are bounded to the ACTIVE index version: a chunk is
 * only searchable when its embedding exists for that version. When no ACTIVE index exists
 * the retrieval is empty and carries a diagnostic for the caller (assistant fallback
 * "chưa đủ dữ liệu" or admin test screen).
 */
@Service
public class KnowledgeRetrievalService implements KnowledgeRetrievalPortIn {

    public static final String DIAG_FEATURE_DISABLED = "FEATURE_DISABLED";
    public static final String DIAG_NO_ACTIVE_KNOWLEDGE_INDEX = "NO_ACTIVE_KNOWLEDGE_INDEX";
    public static final String DIAG_INDEX_CONFIG_INVALID = "INDEX_CONFIG_INVALID";
    public static final String DIAG_EMBEDDING_FAILED = "EMBEDDING_FAILED";

    private static final Logger log = LoggerFactory.getLogger(KnowledgeRetrievalService.class);

    private final KnowledgeRetrievalPortOut retrievalPortOut;
    private final KnowledgeIndexVersionPortOut indexVersionPortOut;
    private final AiModelConfigurationPortOut configurationPortOut;
    private final EmbeddingService embeddingService;
    private final EmbeddingPromptFormatter promptFormatter;
    private final PiiRedactionService piiRedactionService;
    private final EmbeddingProperties properties;
    private final KnowledgeAccessContextResolver accessContextResolver;

    public KnowledgeRetrievalService(
            KnowledgeRetrievalPortOut retrievalPortOut,
            KnowledgeIndexVersionPortOut indexVersionPortOut,
            AiModelConfigurationPortOut configurationPortOut,
            EmbeddingService embeddingService,
            EmbeddingPromptFormatter promptFormatter,
            PiiRedactionService piiRedactionService,
            EmbeddingProperties properties,
            KnowledgeAccessContextResolver accessContextResolver
    ) {
        this.retrievalPortOut = retrievalPortOut;
        this.indexVersionPortOut = indexVersionPortOut;
        this.configurationPortOut = configurationPortOut;
        this.embeddingService = embeddingService;
        this.promptFormatter = promptFormatter;
        this.piiRedactionService = piiRedactionService;
        this.properties = properties;
        this.accessContextResolver = accessContextResolver;
    }

    @Override
    public KnowledgeRetrievalResult searchForCurrentUser(String query, int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 50));
        return search(null, query, accessContextResolver.resolveScopes(), safeLimit);
    }

    public KnowledgeRetrievalResult search(UUID tenantId, String query, List<String> accessScopes, int limit) {
        if (query == null || query.isBlank() || !properties.isEnabled() || !properties.isVectorSearchEnabled()) {
            return KnowledgeRetrievalResult.empty(DIAG_FEATURE_DISABLED);
        }
        KnowledgeIndexVersion active = indexVersionPortOut.findActive().orElse(null);
        if (active == null) {
            return KnowledgeRetrievalResult.empty(DIAG_NO_ACTIVE_KNOWLEDGE_INDEX);
        }
        AiModelConfiguration configuration = findUsableEmbeddingConfiguration(active);
        if (configuration == null || !promptFormatter.supports(active.getEmbeddingPromptVersion())) {
            return KnowledgeRetrievalResult.empty(DIAG_INDEX_CONFIG_INVALID);
        }
        try {
            String redactedQuery = piiRedactionService.redact(query).value();
            String formatted = promptFormatter.formatQuery(active.getEmbeddingPromptVersion(), redactedQuery);
            EmbeddingResult result = embeddingService.embed(new EmbeddingRequest(formatted, null), configuration);
            if (!result.isSuccess() || result.getVector().dimension() != active.getDimension()) {
                return boundedLexicalFallback(tenantId, query, accessScopes, active, limit, DIAG_EMBEDDING_FAILED);
            }
            List<KnowledgeSearchResult> vectorResults = retrievalPortOut.searchVector(
                    tenantId,
                    result.getVector(),
                    accessScopes,
                    active.getIndexVersionId(),
                    limit
            );
            if (vectorResults.isEmpty()) {
                return boundedLexicalFallback(tenantId, query, accessScopes, active, limit, null);
            }
            if (!properties.isLexicalFallbackEnabled()) {
                return KnowledgeRetrievalResult.ok(active.getIndexVersionId(), vectorResults);
            }
            return KnowledgeRetrievalResult.ok(active.getIndexVersionId(),
                    merge(vectorResults, lexicalSearch(tenantId, query, accessScopes, active.getIndexVersionId(), limit), limit));
        } catch (Exception exception) {
            log.warn("Vector search failed, falling back to lexical search", exception);
            return boundedLexicalFallback(tenantId, query, accessScopes, active, limit, DIAG_EMBEDDING_FAILED);
        }
    }

    private AiModelConfiguration findUsableEmbeddingConfiguration(KnowledgeIndexVersion active) {
        AiModelConfiguration configuration = configurationPortOut.findById(active.getModelConfigurationId()).orElse(null);
        if (configuration == null
                || configuration.getUseCase() != AiUseCase.EMBEDDING
                || configuration.getOutputDimension() == null
                || configuration.getOutputDimension() != active.getDimension()
                || configuration.getStatus() == AiModelStatus.DISABLED
                || configuration.getProvider() != active.getProvider()
                || !configuration.getModelId().equals(active.getModelId())) {
            return null;
        }
        return configuration;
    }

    private KnowledgeRetrievalResult boundedLexicalFallback(
            UUID tenantId,
            String query,
            List<String> accessScopes,
            KnowledgeIndexVersion active,
            int limit,
            String fallbackDiagnostic
    ) {
        List<KnowledgeSearchResult> results = lexicalSearch(
                tenantId, query, accessScopes, active.getIndexVersionId(), limit);
        if (!results.isEmpty()) {
            return KnowledgeRetrievalResult.ok(active.getIndexVersionId(), results);
        }
        return KnowledgeRetrievalResult.empty(fallbackDiagnostic);
    }

    private List<KnowledgeSearchResult> lexicalSearch(
            UUID tenantId, String query, List<String> accessScopes, UUID indexVersionId, int limit) {
        if (!properties.isLexicalFallbackEnabled()) {
            return List.of();
        }
        return retrievalPortOut.search(tenantId, query, accessScopes, indexVersionId, limit);
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