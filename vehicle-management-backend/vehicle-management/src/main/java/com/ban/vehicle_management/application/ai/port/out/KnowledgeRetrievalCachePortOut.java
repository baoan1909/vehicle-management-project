package com.ban.vehicle_management.application.ai.port.out;

import com.ban.vehicle_management.application.ai.cache.model.CachedRetrievalPayload;
import java.util.Optional;

/**
 * Typed retrieval-evidence cache. Cached rows never carry run, conversation,
 * message or audit identifiers; every hit still persists a fresh audit row.
 */
public interface KnowledgeRetrievalCachePortOut {

    Optional<CachedRetrievalPayload> get(String key);

    void put(String key, CachedRetrievalPayload payload, boolean negative);

    void put(String key, CachedRetrievalPayload payload, boolean negative, String tier);
}
