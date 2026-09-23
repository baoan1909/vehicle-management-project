package com.ban.vehicle_management.infrastructure.mapper.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ban.vehicle_management.domain.ai.model.KnowledgeIndexVersion;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.ai.KnowledgeIndexVersionEntity;
import com.ban.vehicle_management.shared.enumeration.ai.AiProvider;
import com.ban.vehicle_management.shared.enumeration.ai.KnowledgeIndexVersionStatus;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

class KnowledgeIndexVersionPersistenceMapperTest {

    private final KnowledgeIndexVersionPersistenceMapper mapper =
            Mappers.getMapper(KnowledgeIndexVersionPersistenceMapper.class);

    @Test
    void shouldRestoreCompleteDomainSnapshot() {
        KnowledgeIndexVersionEntity entity = new KnowledgeIndexVersionEntity();
        entity.setIndexVersionId(UUID.randomUUID());
        entity.setVersionCode("IDX-GEMINI-2");
        entity.setModelConfigurationId(UUID.randomUUID());
        entity.setProvider(AiProvider.GEMINI);
        entity.setModelId("gemini-embedding-2");
        entity.setDimension(768);
        entity.setChunkerVersion("chunker-v1");
        entity.setEmbeddingPromptVersion("rag-qa-v1");
        entity.setDistanceMetric("COSINE");
        entity.setNormalization("NONE");
        entity.setStatus(KnowledgeIndexVersionStatus.ACTIVE);
        entity.setExpectedChunkCount(12);
        entity.setEmbeddedChunkCount(12);
        entity.setFailedChunkCount(0);
        entity.setContentChecksum("checksum");
        entity.setCreatedAt(Instant.parse("2026-09-17T01:00:00Z"));
        entity.setActivatedAt(Instant.parse("2026-09-17T02:00:00Z"));
        entity.setVersion(3);

        KnowledgeIndexVersion domain = mapper.toDomain(entity);

        assertEquals(entity.getIndexVersionId(), domain.getIndexVersionId());
        assertEquals("IDX-GEMINI-2", domain.getVersionCode());
        assertEquals(entity.getModelConfigurationId(), domain.getModelConfigurationId());
        assertEquals(AiProvider.GEMINI, domain.getProvider());
        assertEquals("gemini-embedding-2", domain.getModelId());
        assertEquals(768, domain.getDimension());
        assertEquals("rag-qa-v1", domain.getEmbeddingPromptVersion());
        assertEquals(KnowledgeIndexVersionStatus.ACTIVE, domain.getStatus());
        assertEquals(12, domain.getExpectedChunkCount());
        assertEquals(entity.getActivatedAt(), domain.getActivatedAt());
        assertEquals(3, domain.getVersion());
    }

    @Test
    void shouldTreatAssignedUuidAsNewUntilHibernateInitializesVersion() {
        KnowledgeIndexVersion draft = KnowledgeIndexVersion.draft(
                "IDX-GEMINI-NEW",
                UUID.randomUUID(),
                AiProvider.GEMINI,
                "gemini-embedding-2",
                768,
                "chunker-v1",
                "rag-qa-v1",
                "COSINE",
                "NONE",
                "checksum",
                UUID.randomUUID(),
                Instant.parse("2026-09-23T08:00:00Z"));

        KnowledgeIndexVersionEntity entity = mapper.toEntity(draft);

        assertTrue(entity.isNew());
        entity.setVersion(0);
        assertFalse(entity.isNew());
    }
}
