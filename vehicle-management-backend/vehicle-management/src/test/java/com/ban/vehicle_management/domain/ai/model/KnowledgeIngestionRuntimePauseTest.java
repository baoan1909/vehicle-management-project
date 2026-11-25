package com.ban.vehicle_management.domain.ai.model;

import static org.junit.jupiter.api.Assertions.*;
import com.ban.vehicle_management.shared.enumeration.ai.*;
import com.ban.vehicle_management.domain.ai.knowledge.model.KnowledgeIngestionJob;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class KnowledgeIngestionRuntimePauseTest {
    @Test void runtimePauseKeepsCheckpointAndDoesNotSpendProviderRetry() {
        Instant now=Instant.now();var job=KnowledgeIngestionJob.create(UUID.randomUUID(),3,"idempotent",UUID.randomUUID(),now);
        job.claim("worker",1,now,now.plusSeconds(60));job.setLastCompletedStage(IngestionStage.CHUNKING);
        job.pauseForRuntime(now.plusSeconds(5));
        assertEquals(KnowledgeIngestionJobStatus.RETRYING,job.getStatus());
        assertEquals(IngestionStage.CHUNKING,job.getLastCompletedStage());
        assertEquals(0,job.getAttemptCount());assertNull(job.getLockedBy());assertNull(job.getLockExpiresAt());
        assertEquals("AI_RUNTIME_PAUSED",job.getErrorCode());assertEquals("idempotent",job.getIdempotencyKey());
    }
}
