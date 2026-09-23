package com.ban.vehicle_management.infrastructure.persistence.database.repository.ai;

import com.ban.vehicle_management.domain.ai.model.EmbeddingVector;
import com.ban.vehicle_management.domain.ai.model.HybridSearchRow;
import com.ban.vehicle_management.domain.ai.model.KnowledgeChunk;
import com.ban.vehicle_management.domain.ai.model.KnowledgeSearchResult;
import com.ban.vehicle_management.domain.ai.knowledge.model.KnowledgeChunkDraft;
import com.ban.vehicle_management.domain.ai.knowledge.model.KnowledgeChunkSnapshot;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class KnowledgeChunkRepository {

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Lexical search bounded to the ACTIVE index version: a chunk is only considered
     * when it is a member of that index (an embedding row exists). This keeps lexical
     * and vector retrieval aligned to the same activated corpus.
     */
    public List<KnowledgeSearchResult> hybridSearch(
            UUID tenantId,
            String query,
            List<String> accessScopes,
            UUID indexVersionId,
            int limit
    ) {
        String sql = """
                SELECT document.document_id,
                       chunk.chunk_id,
                       chunk.title,
                       chunk.content,
                       chunk.summary,
                       chunk.source_page,
                       chunk.source_section,
                       ts_rank(chunk.search_vector, plainto_tsquery('simple', :query)) AS score
                FROM ai.knowledge_chunks chunk
                JOIN ai.knowledge_documents document ON document.document_id = chunk.document_id
                JOIN ai.knowledge_sources source ON source.source_id = document.source_id
                WHERE chunk.status = 'READY'
                  AND document.status = 'READY'
                  AND source.status = 'ACTIVE'
                  AND chunk.access_scope IN (:accessScopes)
                  AND (
                      (CAST(:tenantId AS uuid) IS NULL AND chunk.tenant_id IS NULL)
                      OR (CAST(:tenantId AS uuid) IS NOT NULL
                          AND (chunk.tenant_id IS NULL OR chunk.tenant_id = CAST(:tenantId AS uuid)))
                  )
                  AND chunk.search_vector @@ plainto_tsquery('simple', :query)
                  AND EXISTS (
                      SELECT 1
                      FROM ai.knowledge_embeddings embedding
                      WHERE embedding.chunk_id = chunk.chunk_id
                        AND embedding.index_version_id = :indexVersionId
                  )
                ORDER BY score DESC, chunk.created_at DESC
                LIMIT :limit
                """;
        @SuppressWarnings("unchecked")
        List<Object[]> rows = entityManager.createNativeQuery(sql)
                .setParameter("tenantId", tenantId)
                .setParameter("query", query)
                .setParameter("accessScopes", accessScopes)
                .setParameter("indexVersionId", indexVersionId)
                .setParameter("limit", limit)
                .getResultList();
        return rows.stream()
                .map(row -> new KnowledgeSearchResult(
                        (UUID) row[0],
                        (UUID) row[1],
                        (String) row[2],
                        (String) row[3],
                        (String) row[4],
                        (Integer) row[5],
                        (String) row[6],
                        row[7] instanceof BigDecimal value ? value : BigDecimal.ZERO
                ))
                .toList();
    }

    public List<KnowledgeSearchResult> vectorSearch(
            UUID tenantId,
            EmbeddingVector queryVector,
            List<String> accessScopes,
            UUID indexVersionId,
            int limit
    ) {
        String queryLiteral = KnowledgeEmbeddingRepository.toVectorLiteral(queryVector);
        String sql = """
                SELECT document.document_id,
                       chunk.chunk_id,
                       chunk.title,
                       chunk.content,
                       chunk.summary,
                       chunk.source_page,
                       chunk.source_section,
                       (1 - (embedding.embedding <=> CAST(:queryVector AS vector)))::numeric(8,5) AS score
                FROM ai.knowledge_embeddings embedding
                JOIN ai.knowledge_chunks chunk ON chunk.chunk_id = embedding.chunk_id
                JOIN ai.knowledge_documents document ON document.document_id = chunk.document_id
                JOIN ai.knowledge_sources source ON source.source_id = document.source_id
                WHERE embedding.index_version_id = :indexVersionId
                  AND chunk.status = 'READY'
                  AND document.status = 'READY'
                  AND source.status = 'ACTIVE'
                  AND chunk.access_scope IN (:accessScopes)
                  AND (
                      (CAST(:tenantId AS uuid) IS NULL AND chunk.tenant_id IS NULL)
                      OR (CAST(:tenantId AS uuid) IS NOT NULL
                          AND (chunk.tenant_id IS NULL OR chunk.tenant_id = CAST(:tenantId AS uuid)))
                  )
                ORDER BY embedding.embedding <=> CAST(:queryVector AS vector)
                LIMIT :limit
                """;
        @SuppressWarnings("unchecked")
        List<Object[]> rows = entityManager.createNativeQuery(sql)
                .setParameter("tenantId", tenantId)
                .setParameter("queryVector", queryLiteral)
                .setParameter("accessScopes", accessScopes)
                .setParameter("indexVersionId", indexVersionId)
                .setParameter("limit", limit)
                .getResultList();
        return rows.stream()
                .map(row -> new KnowledgeSearchResult(
                        (UUID) row[0],
                        (UUID) row[1],
                        (String) row[2],
                        (String) row[3],
                        (String) row[4],
                        (Integer) row[5],
                        (String) row[6],
                        row[7] instanceof BigDecimal value ? value : BigDecimal.ZERO
                ))
                .toList();
    }

    /**
     * Single-statement hybrid retrieval. Both branches read the same snapshot
     * with identical filters (status, scope, strict single-tenant, effective
     * dates, latest READY version per document key, ACTIVE index membership),
     * fuse with weighted RRF, cap chunks per document and order deterministically.
     */
    @SuppressWarnings("unchecked")
    public List<HybridSearchRow> hybridSearch(
            UUID tenantId,
            UUID indexVersionId,
            String queryVectorLiteral,
            String normalizedQuery,
            List<String> accessScopes,
            int vectorCandidateLimit,
            int lexicalCandidateLimit,
            double minimumVectorScore,
            double minimumLexicalScore,
            int rrfRankConstant,
            double weightVector,
            double weightLexical,
            int maxChunksPerDocument,
            int finalTopK
    ) {
        String sql = """
                WITH corpus AS (
                    SELECT chunk.chunk_id,
                           chunk.document_id,
                           chunk.title,
                           chunk.content,
                           chunk.summary,
                           chunk.source_page,
                           chunk.source_section
                    FROM ai.knowledge_chunks chunk
                    JOIN ai.knowledge_documents document ON document.document_id = chunk.document_id
                    JOIN ai.knowledge_sources source ON source.source_id = document.source_id
                    WHERE chunk.status = 'READY'
                      AND document.status = 'READY'
                      AND source.status = 'ACTIVE'
                      AND chunk.access_scope IN (:accessScopes)
                      AND (
                          (CAST(:tenantId AS uuid) IS NULL AND chunk.tenant_id IS NULL)
                          OR (CAST(:tenantId AS uuid) IS NOT NULL
                              AND (chunk.tenant_id IS NULL OR chunk.tenant_id = CAST(:tenantId AS uuid)))
                      )
                      AND (document.effective_from IS NULL OR document.effective_from <= now())
                      AND (document.effective_to IS NULL OR document.effective_to > now())
                      AND document.document_version = (
                          SELECT MAX(d2.document_version)
                          FROM ai.knowledge_documents d2
                          WHERE d2.document_key = document.document_key
                            AND d2.status = 'READY'
                      )
                      AND EXISTS (
                          SELECT 1
                          FROM ai.knowledge_embeddings embedding
                          WHERE embedding.chunk_id = chunk.chunk_id
                            AND embedding.index_version_id = :indexVersionId
                      )
                ),
                vector_branch AS (
                    SELECT corpus.*,
                           (1 - (embedding.embedding <=> CAST(:queryVector AS vector)))::numeric AS vector_score,
                           ROW_NUMBER() OVER (
                               ORDER BY embedding.embedding <=> CAST(:queryVector AS vector), corpus.chunk_id
                           ) AS vector_rank
                    FROM corpus
                    JOIN ai.knowledge_embeddings embedding
                      ON embedding.chunk_id = corpus.chunk_id
                     AND embedding.index_version_id = :indexVersionId
                    WHERE vector_norm(embedding.embedding) > 0
                      AND (1 - (embedding.embedding <=> CAST(:queryVector AS vector))) >= :minimumVectorScore
                    ORDER BY embedding.embedding <=> CAST(:queryVector AS vector), corpus.chunk_id
                    LIMIT :vectorLimit
                ),
                lexical_branch AS (
                    SELECT corpus.*,
                           ts_rank(corpus_search.search_vector_unaccent,
                               plainto_tsquery('simple', ai.immutable_unaccent(:normalizedQuery)))::numeric AS lexical_score,
                           ROW_NUMBER() OVER (
                               ORDER BY ts_rank(corpus_search.search_vector_unaccent,
                                   plainto_tsquery('simple', ai.immutable_unaccent(:normalizedQuery))) DESC,
                                   corpus.chunk_id
                           ) AS lexical_rank
                    FROM corpus
                    JOIN ai.knowledge_chunks corpus_search ON corpus_search.chunk_id = corpus.chunk_id
                    WHERE corpus_search.search_vector_unaccent
                        @@ plainto_tsquery('simple', ai.immutable_unaccent(:normalizedQuery))
                      AND ts_rank(corpus_search.search_vector_unaccent,
                              plainto_tsquery('simple', ai.immutable_unaccent(:normalizedQuery)))
                          >= :minimumLexicalScore
                    ORDER BY ts_rank(corpus_search.search_vector_unaccent,
                                 plainto_tsquery('simple', ai.immutable_unaccent(:normalizedQuery))) DESC,
                             corpus.chunk_id
                    LIMIT :lexicalLimit
                ),
                fused AS (
                    SELECT COALESCE(vector_branch.chunk_id, lexical_branch.chunk_id) AS chunk_id,
                           COALESCE(vector_branch.document_id, lexical_branch.document_id) AS document_id,
                           COALESCE(vector_branch.title, lexical_branch.title) AS title,
                           COALESCE(vector_branch.content, lexical_branch.content) AS content,
                           COALESCE(vector_branch.summary, lexical_branch.summary) AS summary,
                           COALESCE(vector_branch.source_page, lexical_branch.source_page) AS source_page,
                           COALESCE(vector_branch.source_section, lexical_branch.source_section) AS source_section,
                           vector_branch.vector_rank AS vector_rank,
                           lexical_branch.lexical_rank AS lexical_rank,
                           vector_branch.vector_score AS vector_score,
                           lexical_branch.lexical_score AS lexical_score,
                           (COALESCE(:weightVector / (:rrfK + vector_branch.vector_rank), 0)
                               + COALESCE(:weightLexical / (:rrfK + lexical_branch.lexical_rank), 0))::numeric(12, 8) AS fused_score
                    FROM vector_branch
                    FULL OUTER JOIN lexical_branch ON lexical_branch.chunk_id = vector_branch.chunk_id
                ),
                capped AS (
                    SELECT fused.*,
                           ROW_NUMBER() OVER (
                               PARTITION BY fused.document_id
                               ORDER BY fused.fused_score DESC, fused.chunk_id
                           ) AS document_rank
                    FROM fused
                )
                SELECT chunk_id,
                       document_id,
                       title,
                       content,
                       summary,
                       source_page,
                       source_section,
                       vector_rank,
                       lexical_rank,
                       vector_score,
                       lexical_score,
                       fused_score,
                       ROW_NUMBER() OVER (ORDER BY fused_score DESC, chunk_id) AS final_rank
                FROM capped
                WHERE document_rank <= :maxChunksPerDocument
                ORDER BY fused_score DESC, chunk_id
                LIMIT :finalTopK
                """;
        List<Object[]> rows = entityManager.createNativeQuery(sql)
                .setParameter("tenantId", tenantId)
                .setParameter("indexVersionId", indexVersionId)
                .setParameter("queryVector", queryVectorLiteral)
                .setParameter("normalizedQuery", normalizedQuery)
                .setParameter("accessScopes", accessScopes)
                .setParameter("minimumVectorScore", minimumVectorScore)
                .setParameter("minimumLexicalScore", minimumLexicalScore)
                .setParameter("vectorLimit", vectorCandidateLimit)
                .setParameter("lexicalLimit", lexicalCandidateLimit)
                .setParameter("rrfK", (double) rrfRankConstant)
                .setParameter("weightVector", weightVector)
                .setParameter("weightLexical", weightLexical)
                .setParameter("maxChunksPerDocument", maxChunksPerDocument)
                .setParameter("finalTopK", finalTopK)
                .getResultList();
        return rows.stream()
                .map(row -> new HybridSearchRow(
                        (UUID) row[1],
                        (UUID) row[0],
                        (String) row[2],
                        (String) row[3],
                        (String) row[4],
                        row[5] == null ? null : ((Number) row[5]).intValue(),
                        (String) row[6],
                        row[7] == null ? null : ((Number) row[7]).intValue(),
                        row[8] == null ? null : ((Number) row[8]).intValue(),
                        toBigDecimal(row[9]),
                        toBigDecimal(row[10]),
                        toBigDecimal(row[11]),
                        row[12] == null ? 0 : ((Number) row[12]).intValue()))
                .toList();
    }

    private static java.math.BigDecimal toBigDecimal(Object value) {
        if (value instanceof java.math.BigDecimal decimal) {
            return decimal;
        }
        if (value instanceof Number number) {
            return java.math.BigDecimal.valueOf(number.doubleValue());
        }
        return java.math.BigDecimal.ZERO;
    }

    public long countEligibleChunks() {
        Object result = entityManager.createNativeQuery("""
                SELECT COUNT(*)
                FROM ai.knowledge_chunks chunk
                JOIN ai.knowledge_documents document ON document.document_id = chunk.document_id
                JOIN ai.knowledge_sources source ON source.source_id = document.source_id
                WHERE chunk.status = 'READY'
                  AND document.status = 'READY'
                  AND source.status = 'ACTIVE'
                  AND chunk.access_scope <> 'TENANT_PRIVATE'
                  AND document.document_version = (
                      SELECT MAX(d2.document_version)
                      FROM ai.knowledge_documents d2
                      WHERE d2.document_key = document.document_key
                        AND d2.status = 'READY'
                  )
                  AND NOT EXISTS (
                      SELECT 1
                      FROM ai.knowledge_documents latest
                      WHERE latest.document_key = document.document_key
                        AND latest.status = 'ARCHIVED'
                        AND latest.document_version = (
                            SELECT MAX(all_versions.document_version)
                            FROM ai.knowledge_documents all_versions
                            WHERE all_versions.document_key = document.document_key
                        )
                  )
                """).getSingleResult();
        return ((Number) result).longValue();
    }

    public String calculateEligibleCorpusChecksum() {
        Object result = entityManager.createNativeQuery("""
                SELECT md5(COALESCE(string_agg(
                    chunk.chunk_id::text || ':' || chunk.document_version::text || ':' || md5(chunk.content),
                    '|' ORDER BY chunk.chunk_id
                ), ''))
                FROM ai.knowledge_chunks chunk
                JOIN ai.knowledge_documents document ON document.document_id = chunk.document_id
                JOIN ai.knowledge_sources source ON source.source_id = document.source_id
                WHERE chunk.status = 'READY'
                  AND document.status = 'READY'
                  AND source.status = 'ACTIVE'
                  AND chunk.access_scope <> 'TENANT_PRIVATE'
                  AND document.document_version = (
                      SELECT MAX(d2.document_version)
                      FROM ai.knowledge_documents d2
                      WHERE d2.document_key = document.document_key
                        AND d2.status = 'READY'
                  )
                  AND NOT EXISTS (
                      SELECT 1
                      FROM ai.knowledge_documents latest
                      WHERE latest.document_key = document.document_key
                        AND latest.status = 'ARCHIVED'
                        AND latest.document_version = (
                            SELECT MAX(all_versions.document_version)
                            FROM ai.knowledge_documents all_versions
                            WHERE all_versions.document_key = document.document_key
                        )
                  )
                """).getSingleResult();
        return (String) result;
    }

    @SuppressWarnings("unchecked")
    public List<KnowledgeChunk> findEligibleChunks(UUID indexVersionId, int limit) {
        List<Object[]> rows = entityManager.createNativeQuery("""
                        SELECT chunk.chunk_id,
                               chunk.title,
                               chunk.content,
                               chunk.summary,
                               chunk.document_version
                        FROM ai.knowledge_chunks chunk
                        JOIN ai.knowledge_documents document ON document.document_id = chunk.document_id
                        JOIN ai.knowledge_sources source ON source.source_id = document.source_id
                        WHERE chunk.status = 'READY'
                          AND document.status = 'READY'
                          AND source.status = 'ACTIVE'
                          AND chunk.access_scope <> 'TENANT_PRIVATE'
                          AND document.document_version = (
                              SELECT MAX(d2.document_version)
                              FROM ai.knowledge_documents d2
                              WHERE d2.document_key = document.document_key
                                AND d2.status = 'READY'
                          )
                          AND NOT EXISTS (
                              SELECT 1
                              FROM ai.knowledge_documents latest
                              WHERE latest.document_key = document.document_key
                                AND latest.status = 'ARCHIVED'
                                AND latest.document_version = (
                                    SELECT MAX(all_versions.document_version)
                                    FROM ai.knowledge_documents all_versions
                                    WHERE all_versions.document_key = document.document_key
                                )
                          )
                          AND NOT EXISTS (
                              SELECT 1
                              FROM ai.knowledge_embeddings embedding
                              WHERE embedding.chunk_id = chunk.chunk_id
                                AND embedding.index_version_id = :indexVersionId
                          )
                        ORDER BY chunk.created_at
                        LIMIT :limit
                        """)
                .setParameter("indexVersionId", indexVersionId)
                .setParameter("limit", limit)
                .getResultList();
        return rows.stream()
                .map(row -> new KnowledgeChunk(
                        (UUID) row[0],
                        (String) row[1],
                        (String) row[2],
                        (String) row[3],
                        row[4] == null ? null : ((Number) row[4]).intValue()))
                .toList();
    }

    @Transactional
    public void replaceChunksForDocument(
            UUID documentId,
            int documentVersion,
            String chunkerVersion,
            List<KnowledgeChunkDraft> chunks) {
        entityManager.createNativeQuery("""
                        DELETE FROM ai.knowledge_chunks
                        WHERE document_id = :documentId
                          AND document_version = :documentVersion
                        """)
                .setParameter("documentId", documentId)
                .setParameter("documentVersion", documentVersion)
                .executeUpdate();
        for (KnowledgeChunkDraft chunk : chunks) {
            entityManager.createNativeQuery("""
                            INSERT INTO ai.knowledge_chunks (
                                document_id,
                                tenant_id,
                                title,
                                content,
                                summary,
                                source_page,
                                source_section,
                                chunk_index,
                                access_scope,
                                document_version,
                                status,
                                token_count,
                                content_hash,
                                heading_path,
                                start_block_index,
                                end_block_index,
                                chunker_version,
                                created_at
                            )
                            SELECT document.document_id,
                                   document.tenant_id,
                                   :title,
                                   :content,
                                   :summary,
                                   :sourcePage,
                                   :sourceSection,
                                   :chunkIndex,
                                   document.access_scope,
                                   :documentVersion,
                                   'DRAFT',
                                   :tokenCount,
                                   :contentHash,
                                   :headingPath,
                                   :startBlockIndex,
                                   :endBlockIndex,
                                   :chunkerVersion,
                                   now()
                            FROM ai.knowledge_documents document
                            WHERE document.document_id = :documentId
                            """)
                    .setParameter("documentId", documentId)
                    .setParameter("documentVersion", documentVersion)
                    .setParameter("title", chunk.getTitle())
                    .setParameter("content", chunk.getContent())
                    .setParameter("summary", chunk.getSummary())
                    .setParameter("sourcePage", chunk.getSourcePage())
                    .setParameter("sourceSection", chunk.getSourceSection())
                    .setParameter("chunkIndex", chunk.getChunkIndex())
                    .setParameter("tokenCount", chunk.getTokenCount())
                    .setParameter("contentHash", chunk.getContentHash())
                    .setParameter("headingPath", chunk.getHeadingPath())
                    .setParameter("startBlockIndex", chunk.getStartBlockIndex())
                    .setParameter("endBlockIndex", chunk.getEndBlockIndex())
                    .setParameter("chunkerVersion", chunkerVersion)
                    .executeUpdate();
        }
    }

    @SuppressWarnings("unchecked")
    public List<KnowledgeChunkSnapshot> findByDocumentIdAndDocVersion(UUID documentId, int documentVersion) {
        List<Object[]> rows = entityManager.createNativeQuery("""
                        SELECT chunk.chunk_id,
                               chunk.document_id,
                               chunk.document_version,
                               chunk.title,
                               chunk.content,
                               chunk.summary,
                               chunk.source_page,
                               chunk.source_section,
                               chunk.chunk_index,
                               chunk.token_count,
                               chunk.heading_path
                        FROM ai.knowledge_chunks chunk
                        WHERE chunk.document_id = :documentId
                          AND chunk.document_version = :documentVersion
                        ORDER BY chunk.chunk_index
                        """)
                .setParameter("documentId", documentId)
                .setParameter("documentVersion", documentVersion)
                .getResultList();
        return rows.stream()
                .map(row -> KnowledgeChunkSnapshot.of(
                        (UUID) row[0],
                        (UUID) row[1],
                        row[2] == null ? null : ((Number) row[2]).intValue(),
                        (String) row[3],
                        (String) row[4],
                        (String) row[5],
                        row[6] == null ? null : ((Number) row[6]).intValue(),
                        (String) row[7],
                        row[8] == null ? null : ((Number) row[8]).intValue(),
                        row[9] == null ? null : ((Number) row[9]).intValue(),
                        (String) row[10]))
                .toList();
    }

    @Transactional
    public int updateStatusByDocumentId(UUID documentId, String fromStatus, String toStatus) {
        return entityManager.createNativeQuery("""
                        UPDATE ai.knowledge_chunks
                        SET status = :toStatus,
                            updated_at = now()
                        WHERE document_id = :documentId
                          AND status = :fromStatus
                        """)
                .setParameter("documentId", documentId)
                .setParameter("fromStatus", fromStatus)
                .setParameter("toStatus", toStatus)
                .executeUpdate();
    }

    public long countByDocumentId(UUID documentId) {
        Object result = entityManager.createNativeQuery("""
                        SELECT COUNT(*)
                        FROM ai.knowledge_chunks
                        WHERE document_id = :documentId
                        """)
                .setParameter("documentId", documentId)
                .getSingleResult();
        return ((Number) result).longValue();
    }
}
