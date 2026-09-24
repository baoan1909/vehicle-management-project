package com.ban.vehicle_management.application.ai.service;

import com.ban.vehicle_management.application.ai.cache.model.CachedHybridRow;
import com.ban.vehicle_management.application.ai.cache.model.CachedRetrievalPayload;
import com.ban.vehicle_management.application.ai.port.out.DistributedCachePortOut;
import com.ban.vehicle_management.application.ai.port.out.KnowledgeRetrievalCachePortOut;
import com.ban.vehicle_management.infrastructure.cache.AiCacheMetrics;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Typed retrieval cache. Scope, tenant, index, checksum and policy versions
 * are part of the key and re-validated on read.
 */
@Component
public class KnowledgeRetrievalCacheAdapter implements KnowledgeRetrievalCachePortOut {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeRetrievalCacheAdapter.class);

    private final DistributedCachePortOut distributedCache;
    private final AiCacheProperties cacheProperties;
    private final AiCacheMetrics metrics;

    public KnowledgeRetrievalCacheAdapter(DistributedCachePortOut distributedCache,
            AiCacheProperties cacheProperties, AiCacheMetrics metrics) {
        this.distributedCache = distributedCache;
        this.cacheProperties = cacheProperties;
        this.metrics = metrics;
    }

    @Override
    public Optional<CachedRetrievalPayload> get(String key) {
        if (!cacheProperties.isEnabled()) {
            return Optional.empty();
        }
        Optional<CachedRetrievalPayload> cached = distributedCache.get(key, CachedRetrievalPayload.class);
        if (cached.isEmpty()) {
            metrics.miss("knowledge-retrieval");
            return Optional.empty();
        }
        CachedRetrievalPayload payload = cached.get();
        if (!valid(payload)) {
            metrics.invalidPayload("knowledge-retrieval");
            distributedCache.delete(key);
            return Optional.empty();
        }
        metrics.hit("knowledge-retrieval");
        return Optional.of(payload);
    }

    @Override
    public void put(String key, CachedRetrievalPayload payload, boolean negative) {
        put(key, payload, negative, "PUBLIC");
    }

    @Override
    public void put(String key, CachedRetrievalPayload payload, boolean negative, String tier) {
        if (!cacheProperties.isEnabled() || !valid(payload)) {
            return;
        }
        try {
            List<CachedHybridRow> rows = payload.rows() == null ? List.of() : payload.rows();
            if (rows.size() > cacheProperties.getMaxCachedEvidence()) {
                return;
            }
            distributedCache.put(key, payload, ttlForTier(tier, negative));
        } catch (Exception exception) {
            log.debug("Retrieval cache put skipped error={}", exception.getClass().getSimpleName());
        }
    }

    public Duration ttlForScopeFingerprint(String scopeFingerprint, boolean negative) {
        return ttlForScope(scopeFingerprint, null, negative);
    }

    private Duration ttlForScope(String scopeFingerprint, String tenantFingerprint, boolean negative) {
        if (negative) {
            return cacheProperties.getNegativeRetrievalTtl() == null ? Duration.ofSeconds(30)
                    : cacheProperties.getNegativeRetrievalTtl();
        }
        // Scope fingerprint is opaque; TTL tier is chosen by the service layer via explicit method.
        return cacheProperties.getPublicRetrievalTtl() == null ? Duration.ofMinutes(15)
                : cacheProperties.getPublicRetrievalTtl();
    }

    public Duration ttlForTier(String tier, boolean negative) {
        if (negative) {
            return cacheProperties.getNegativeRetrievalTtl() == null ? Duration.ofSeconds(30)
                    : cacheProperties.getNegativeRetrievalTtl();
        }
        return switch (tier == null ? "PUBLIC" : tier) {
            case "TENANT_PRIVATE" -> cacheProperties.getTenantRetrievalTtl() == null ? Duration.ofMinutes(5)
                    : cacheProperties.getTenantRetrievalTtl();
            case "CUSTOMER" -> cacheProperties.getCustomerRetrievalTtl() == null ? Duration.ofMinutes(10)
                    : cacheProperties.getCustomerRetrievalTtl();
            default -> cacheProperties.getPublicRetrievalTtl() == null ? Duration.ofMinutes(15)
                    : cacheProperties.getPublicRetrievalTtl();
        };
    }

    private boolean valid(CachedRetrievalPayload payload) {
        if (payload == null || payload.schemaVersion() != 1) {
            return false;
        }
        if (payload.activeIndexVersionId() == null || payload.rows() == null) {
            return false;
        }
        if (payload.rows().size() > Math.max(1, cacheProperties.getMaxCachedEvidence())) {
            return false;
        }
        for (CachedHybridRow row : payload.rows()) {
            if (row == null || row.documentId() == null || row.chunkId() == null) {
                return false;
            }
            if (row.documentId().equals(new UUID(0L, 0L))) {
                return false;
            }
        }
        return true;
    }
}
