package com.ban.vehicle_management.infrastructure.persistence.database.repository.ai;

import com.ban.vehicle_management.infrastructure.persistence.database.entity.ai.KnowledgeIndexVersionEntity;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

public interface KnowledgeQualityMetricsRepository extends Repository<KnowledgeIndexVersionEntity, UUID> {

    interface StatusCountView {
        String getStatus();
        long getTotal();
    }

    @Query(value = "SELECT status, COUNT(*) AS total FROM ai.knowledge_sources GROUP BY status", nativeQuery = true)
    List<StatusCountView> countSourcesByStatus();

    @Query(value = "SELECT status, COUNT(*) AS total FROM ai.knowledge_documents GROUP BY status", nativeQuery = true)
    List<StatusCountView> countDocumentsByStatus();

    @Query(value = "SELECT status, COUNT(*) AS total FROM ai.knowledge_ingestion_jobs GROUP BY status", nativeQuery = true)
    List<StatusCountView> countJobsByStatus();

    @Query(value = "SELECT status, COUNT(*) AS total FROM ai.knowledge_index_versions GROUP BY status", nativeQuery = true)
    List<StatusCountView> countIndexVersionsByStatus();

    @Query(value = """
            SELECT COUNT(*)
            FROM ai.knowledge_ingestion_jobs
            WHERE status IN ('PENDING', 'PROCESSING', 'RETRYING', 'REVIEW')
              AND (updated_at IS NULL OR updated_at < :cutoff)
            """, nativeQuery = true)
    long countStuckJobs(@Param("cutoff") Instant cutoff);

    @Query(value = """
            SELECT COUNT(*)
            FROM ai.knowledge_index_versions
            WHERE status IN ('DRAFT', 'BUILDING')
              AND COALESCE(updated_at, created_at) < :cutoff
            """, nativeQuery = true)
    long countStaleCandidates(@Param("cutoff") Instant cutoff);

    @Query(value = """
            SELECT COUNT(*)
            FROM ai.knowledge_documents
            WHERE status = 'READY'
              AND effective_to IS NOT NULL
              AND effective_to <= :now
            """, nativeQuery = true)
    long countExpiredDocuments(@Param("now") Instant now);

    @Query(value = """
            SELECT COUNT(*)
            FROM ai.knowledge_chunks chunk
            JOIN ai.knowledge_documents document ON document.document_id = chunk.document_id
            WHERE document.status = 'READY'
              AND NOT EXISTS (
                  SELECT 1
                  FROM ai.knowledge_staged_embeddings staged
                  WHERE staged.chunk_id = chunk.chunk_id
              )
            """, nativeQuery = true)
    long countMissingEmbeddings();

    @Query(value = """
            SELECT COUNT(*)
            FROM ai.knowledge_index_versions
            WHERE status = 'ACTIVE'
              AND (embedded_chunk_count <> expected_chunk_count OR failed_chunk_count > 0)
            """, nativeQuery = true)
    long countIncompleteActiveIndexes();

    @Query(value = """
            SELECT model_configuration_id
            FROM ai.knowledge_index_versions
            WHERE status = 'ACTIVE'
            """, nativeQuery = true)
    List<UUID> findActiveModelConfigurationIds();
}
