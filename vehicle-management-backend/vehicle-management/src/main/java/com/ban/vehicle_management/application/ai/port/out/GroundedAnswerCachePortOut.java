package com.ban.vehicle_management.application.ai.port.out;

import com.ban.vehicle_management.application.ai.cache.model.CachedGroundedAnswer;
import java.util.Optional;

/**
 * Typed grounded-answer cache. Only STATIC_KNOWLEDGE answers that already
 * passed output and citation validation may be stored.
 */
public interface GroundedAnswerCachePortOut {

    Optional<CachedGroundedAnswer> get(String key);

    void put(String key, CachedGroundedAnswer payload);
}
