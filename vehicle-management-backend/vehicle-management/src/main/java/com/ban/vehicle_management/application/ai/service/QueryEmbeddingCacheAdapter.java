package com.ban.vehicle_management.application.ai.service;

import com.ban.vehicle_management.application.ai.cache.model.CachedEmbedding;
import com.ban.vehicle_management.application.ai.port.out.DistributedCachePortOut;
import com.ban.vehicle_management.application.ai.port.out.QueryEmbeddingCachePortOut;
import com.ban.vehicle_management.domain.ai.model.AiModelConfiguration;
import com.ban.vehicle_management.domain.ai.model.EmbeddingVector;
import com.ban.vehicle_management.infrastructure.cache.AiCacheMetrics;
import java.time.Duration;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Typed query-embedding cache with strict validation. Document ingestion never
 * uses this path; only end-user query embeddings are cached.
 */
@Component
public class QueryEmbeddingCacheAdapter implements QueryEmbeddingCachePortOut {

    private static final Logger log = LoggerFactory.getLogger(QueryEmbeddingCacheAdapter.class);

    private final DistributedCachePortOut distributedCache;
    private final AiCacheProperties cacheProperties;
    private final AiCacheMetrics metrics;

    public QueryEmbeddingCacheAdapter(DistributedCachePortOut distributedCache,
            AiCacheProperties cacheProperties, AiCacheMetrics metrics) {
        this.distributedCache = distributedCache;
        this.cacheProperties = cacheProperties;
        this.metrics = metrics;
    }

    @Override
    public Optional<EmbeddingVector> get(String key, int expectedDimension) {
        if (!cacheProperties.isEnabled() || expectedDimension <= 0
                || expectedDimension > cacheProperties.getMaxVectorDimensions()) {
            return Optional.empty();
        }
        Optional<CachedEmbedding> cached = distributedCache.get(key, CachedEmbedding.class);
        if (cached.isEmpty()) {
            metrics.miss("embedding-query");
            return Optional.empty();
        }
        CachedEmbedding payload = cached.get();
        if (payload.values() == null || payload.dimension() != expectedDimension
                || payload.values().length != expectedDimension) {
            metrics.invalidPayload("embedding-query");
            distributedCache.delete(key);
            return Optional.empty();
        }
        try {
            EmbeddingVector vector = EmbeddingVector.of(payload.values(), expectedDimension);
            metrics.hit("embedding-query");
            return Optional.of(vector);
        } catch (IllegalArgumentException exception) {
            metrics.invalidPayload("embedding-query");
            distributedCache.delete(key);
            log.debug("Invalid cached embedding error={}", exception.getClass().getSimpleName());
            return Optional.empty();
        }
    }

    @Override
    public void put(String key, EmbeddingVector vector, AiModelConfiguration configuration) {
        if (!cacheProperties.isEnabled() || vector == null || configuration == null) {
            return;
        }
        if (vector.dimension() > cacheProperties.getMaxVectorDimensions()) {
            return;
        }
        try {
            CachedEmbedding payload = new CachedEmbedding(1,
                    java.time.Instant.now().toString(), vector.values(), vector.dimension());
            distributedCache.put(key, payload, ttl());
        } catch (Exception exception) {
            log.debug("Embedding cache put skipped error={}", exception.getClass().getSimpleName());
        }
    }

    private Duration ttl() {
        Duration base = cacheProperties.getQueryEmbeddingTtl() == null
                ? Duration.ofHours(24) : cacheProperties.getQueryEmbeddingTtl();
        return base;
    }
}
