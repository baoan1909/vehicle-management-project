package com.ban.vehicle_management.application.ai.port.out;

import com.ban.vehicle_management.domain.ai.model.AiModelConfiguration;
import com.ban.vehicle_management.domain.ai.model.EmbeddingVector;
import java.util.Optional;

/**
 * Typed query-embedding cache. Keys already encode provider/model/dimension,
 * prompt and normalization versions.
 */
public interface QueryEmbeddingCachePortOut {

    Optional<EmbeddingVector> get(String key, int expectedDimension);

    void put(String key, EmbeddingVector vector, AiModelConfiguration configuration);
}
