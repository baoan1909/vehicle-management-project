package com.ban.vehicle_management.domain.ai.model;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.ban.vehicle_management.shared.enumeration.ai.AiProvider;
import com.ban.vehicle_management.shared.enumeration.ai.KnowledgeIndexVersionStatus;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class KnowledgeIndexVersionTest {

    private final Instant now = Instant.parse("2026-09-17T10:00:00Z");
    private final UUID actor = UUID.randomUUID();

    private KnowledgeIndexVersion draft() {
        return KnowledgeIndexVersion.draft(
                "IDX-TEST-1",
                UUID.randomUUID(),
                AiProvider.GEMINI,
                "gemini-embedding-2",
                768,
                KnowledgeIndexVersionUseCaseConstants.CHUNKER_VERSION,
                "rag-qa-v1",
                KnowledgeIndexVersionUseCaseConstants.DISTANCE_METRIC,
                KnowledgeIndexVersionUseCaseConstants.NORMALIZATION,
                "checksum",
                actor,
                now
        );
    }

    @Test
    void draftCreatesDraftState() {
        KnowledgeIndexVersion version = draft();
        assertEquals(KnowledgeIndexVersionStatus.DRAFT, version.getStatus());
        assertEquals(0, version.getExpectedChunkCount());
        assertNull(version.getVersion());
    }

    @Test
    void shouldWalkFullLifecycle() {
        KnowledgeIndexVersion version = draft();
        version.setExpectedChunkCount(10);
        version.startBuild(actor, now);
        assertEquals(KnowledgeIndexVersionStatus.BUILDING, version.getStatus());

        version.setEmbeddedChunkCount(10);
        assertDoesNotThrow(() -> version.markReady(now));
        assertEquals(KnowledgeIndexVersionStatus.READY, version.getStatus());

        version.activate(actor, now);
        assertEquals(KnowledgeIndexVersionStatus.ACTIVE, version.getStatus());
        assertEquals(actor, version.getActivatedBy());

        version.retire(actor, now);
        assertEquals(KnowledgeIndexVersionStatus.RETIRED, version.getStatus());

        version.activate(actor, now);
        assertEquals(KnowledgeIndexVersionStatus.ACTIVE, version.getStatus());
    }

    @Test
    void shouldFailBuildWithFailureCode() {
        KnowledgeIndexVersion version = draft();
        version.startBuild(actor, now);
        version.fail("EMBEDDING_RATE_LIMITED", now);
        assertEquals(KnowledgeIndexVersionStatus.FAILED, version.getStatus());
        assertEquals("EMBEDDING_RATE_LIMITED", version.getFailureCode());
    }

    @Test
    void shouldRejectInvalidTransitions() {
        KnowledgeIndexVersion version = draft();
        assertThrows(BadRequestException.class, () -> version.activate(actor, now));
        version.startBuild(actor, now);
        version.fail("EMBEDDING_FAILED", now);
        assertThrows(BadRequestException.class, () -> version.activate(actor, now));
    }

    @Test
    void isBuildCompleteRequiresCoverage() {
        KnowledgeIndexVersion version = draft();
        version.setExpectedChunkCount(5);
        version.setFailedChunkCount(0);
        version.setEmbeddedChunkCount(5);
        assertEquals(true, version.isBuildComplete());

        version.setEmbeddedChunkCount(4);
        assertEquals(false, version.isBuildComplete());
    }

    static final class KnowledgeIndexVersionUseCaseConstants {
        static final String CHUNKER_VERSION = "chunker-v1";
        static final String DISTANCE_METRIC = "COSINE";
        static final String NORMALIZATION = "NONE";

        private KnowledgeIndexVersionUseCaseConstants() {
        }
    }
}
