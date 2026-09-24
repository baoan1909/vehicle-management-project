package com.ban.vehicle_management.application.ai.service;

import com.ban.vehicle_management.application.ai.cache.model.CachedCitation;
import com.ban.vehicle_management.application.ai.cache.model.CachedGroundedAnswer;
import com.ban.vehicle_management.application.ai.port.out.DistributedCachePortOut;
import com.ban.vehicle_management.application.ai.port.out.GroundedAnswerCachePortOut;
import com.ban.vehicle_management.infrastructure.cache.AiCacheMetrics;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Typed grounded-answer cache. Only validated STATIC_KNOWLEDGE answers are
 * stored; every hit still persists a fresh message, citations and audit row.
 */
@Component
public class GroundedAnswerCacheAdapter implements GroundedAnswerCachePortOut {

    private static final Logger log = LoggerFactory.getLogger(GroundedAnswerCacheAdapter.class);

    private final DistributedCachePortOut distributedCache;
    private final AiCacheProperties cacheProperties;
    private final AiCacheMetrics metrics;

    public GroundedAnswerCacheAdapter(DistributedCachePortOut distributedCache,
            AiCacheProperties cacheProperties, AiCacheMetrics metrics) {
        this.distributedCache = distributedCache;
        this.cacheProperties = cacheProperties;
        this.metrics = metrics;
    }

    @Override
    public Optional<CachedGroundedAnswer> get(String key) {
        if (!cacheProperties.isEnabled()) {
            return Optional.empty();
        }
        Optional<CachedGroundedAnswer> cached = distributedCache.get(key, CachedGroundedAnswer.class);
        if (cached.isEmpty()) {
            metrics.miss("grounded-answer");
            return Optional.empty();
        }
        CachedGroundedAnswer payload = cached.get();
        if (!valid(payload)) {
            metrics.invalidPayload("grounded-answer");
            distributedCache.delete(key);
            return Optional.empty();
        }
        metrics.hit("grounded-answer");
        return Optional.of(payload);
    }

    @Override
    public void put(String key, CachedGroundedAnswer payload) {
        if (!cacheProperties.isEnabled() || !valid(payload)) {
            return;
        }
        try {
            distributedCache.put(key, payload, ttlForTier("PUBLIC"));
        } catch (Exception exception) {
            log.debug("Grounded answer cache put skipped error={}", exception.getClass().getSimpleName());
        }
    }

    public void put(String key, CachedGroundedAnswer payload, String tier) {
        if (!cacheProperties.isEnabled() || !valid(payload)) {
            return;
        }
        try {
            distributedCache.put(key, payload, ttlForTier(tier));
        } catch (Exception exception) {
            log.debug("Grounded answer cache put skipped error={}", exception.getClass().getSimpleName());
        }
    }

    public Duration ttlForTier(String tier) {
        return switch (tier == null ? "PUBLIC" : tier) {
            case "TENANT_PRIVATE" -> cacheProperties.getTenantGroundedAnswerTtl() == null ? Duration.ofMinutes(5)
                    : cacheProperties.getTenantGroundedAnswerTtl();
            case "CUSTOMER" -> cacheProperties.getCustomerGroundedAnswerTtl() == null ? Duration.ofMinutes(5)
                    : cacheProperties.getCustomerGroundedAnswerTtl();
            default -> cacheProperties.getPublicGroundedAnswerTtl() == null ? Duration.ofMinutes(15)
                    : cacheProperties.getPublicGroundedAnswerTtl();
        };
    }

    private boolean valid(CachedGroundedAnswer payload) {
        if (payload == null || payload.schemaVersion() != 1) {
            return false;
        }
        if (payload.responseText() == null || payload.responseText().isBlank()
                || payload.responseText().length() > Math.max(100, cacheProperties.getMaxCachedAnswerChars())) {
            return false;
        }
        List<CachedCitation> citations = payload.citations() == null ? List.of() : payload.citations();
        if (citations.size() > Math.max(1, cacheProperties.getMaxCachedCitations())) {
            return false;
        }
        for (CachedCitation citation : citations) {
            if (citation == null || citation.documentId() == null || citation.chunkId() == null
                    || citation.label() == null || !citation.label().matches("C[1-9][0-9]?")) {
                return false;
            }
        }
        return payload.activeIndexVersionId() != null;
    }
}
