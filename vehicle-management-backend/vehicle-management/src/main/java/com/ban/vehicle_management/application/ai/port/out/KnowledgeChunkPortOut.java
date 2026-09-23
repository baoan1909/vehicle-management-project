package com.ban.vehicle_management.application.ai.port.out;

import com.ban.vehicle_management.domain.ai.model.KnowledgeChunk;
import java.util.List;
import java.util.UUID;

/**
 * Eligible knowledge chunks used while building an index. A chunk is eligible when the
 * chunk itself, its document and its source are all active/ready and it does not already
 * carry an embedding for the target index version.
 */
public interface KnowledgeChunkPortOut {

    long countEligibleChunks();

    String calculateEligibleCorpusChecksum();

    List<KnowledgeChunk> findEligibleChunks(UUID indexVersionId, int limit);
}
