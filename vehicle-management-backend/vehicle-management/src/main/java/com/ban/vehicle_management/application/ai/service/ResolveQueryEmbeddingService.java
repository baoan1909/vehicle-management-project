package com.ban.vehicle_management.application.ai.service;

import com.ban.vehicle_management.application.ai.port.out.QueryEmbeddingCachePortOut;
import com.ban.vehicle_management.domain.ai.model.AiModelConfiguration;
import com.ban.vehicle_management.domain.ai.model.EmbeddingRequest;
import com.ban.vehicle_management.domain.ai.model.EmbeddingResult;
import com.ban.vehicle_management.domain.ai.model.EmbeddingVector;
import com.ban.vehicle_management.infrastructure.cache.AiCacheKeyFactory;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Internal query-embedding resolution with fail-open cache. Cache is only used
 * for safe queries (no PII/secrets/identifiers) and only after HMAC readiness
 * is confirmed; otherwise the flow falls back to {@link EmbeddingService}.
 */
@Service
public class ResolveQueryEmbeddingService {

    private static final Logger log = LoggerFactory.getLogger(ResolveQueryEmbeddingService.class);

    private final EmbeddingService embeddingService;
    private final QueryEmbeddingCachePortOut embeddingCache;
    private final AiCacheKeyFactory keyFactory;
    private final SensitiveQueryGuard sensitiveGuard;
    private final AiCacheProperties cacheProperties;

    public ResolveQueryEmbeddingService(EmbeddingService embeddingService,
            QueryEmbeddingCachePortOut embeddingCache,
            AiCacheKeyFactory keyFactory,
            SensitiveQueryGuard sensitiveGuard,
            AiCacheProperties cacheProperties) {
        this.embeddingService = embeddingService;
        this.embeddingCache = embeddingCache;
        this.keyFactory = keyFactory;
        this.sensitiveGuard = sensitiveGuard;
        this.cacheProperties = cacheProperties;
    }

    public EmbeddingResult embedQuery(String formattedQuery, String redactedQuery,
            AiModelConfiguration configuration, int expectedDimension,
            String embeddingPromptVersion) {
        String normalized = null;
        String cacheKey = null;
        boolean cacheable = cacheProperties.isEnabled() && keyFactory.hmacReady()
                && sensitiveGuard.isCacheable(redactedQuery);
        if (cacheable) {
            try {
                normalized = AiCacheKeyFactory.normalizedForKey(redactedQuery);
                cacheKey = keyFactory.embeddingKey(
                        configuration.getProvider() == null ? "GEMINI" : configuration.getProvider().name(),
                        configuration.getModelId(), expectedDimension, embeddingPromptVersion, normalized);
                Optional<EmbeddingVector> cached = embeddingCache.get(cacheKey, expectedDimension);
                if (cached.isPresent()) {
                    return EmbeddingResult.success(cached.get(), configuration.getModelId());
                }
            } catch (Exception exception) {
                log.debug("Embedding cache lookup skipped error={}", exception.getClass().getSimpleName());
                cacheKey = null;
            }
        }
        EmbeddingResult result = embeddingService.embed(new EmbeddingRequest(formattedQuery, null), configuration);
        if (cacheKey != null && result.isSuccess() && result.getVector() != null
                && result.getVector().dimension() == expectedDimension && cacheable) {
            try {
                embeddingCache.put(cacheKey, result.getVector(), configuration);
            } catch (Exception exception) {
                log.debug("Embedding cache store skipped error={}", exception.getClass().getSimpleName());
            }
        }
        return result;
    }
}
