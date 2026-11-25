package com.ban.vehicle_management.infrastructure.persistence.database.repository.ai;

import com.ban.vehicle_management.domain.ai.knowledge.model.KnowledgeChunkDraft;
import com.ban.vehicle_management.domain.ai.knowledge.model.KnowledgeChunkSnapshot;
import com.ban.vehicle_management.domain.ai.model.EmbeddingVector;
import com.ban.vehicle_management.domain.ai.model.HybridSearchRow;
import com.ban.vehicle_management.domain.ai.model.KnowledgeChunk;
import com.ban.vehicle_management.domain.ai.model.KnowledgeSearchResult;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.ai.KnowledgeChunkEntity;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface KnowledgeChunkRepository extends JpaRepository<KnowledgeChunkEntity, UUID> {

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
        update ai.knowledge_chunks
           set status = 'ARCHIVED', updated_at = :now
         where tenant_id in (:tenantIds) and access_scope = 'TENANT_PRIVATE' and status <> 'ARCHIVED'
        """, nativeQuery = true)
    int archiveTenantPrivate(
            @Param("tenantIds") java.util.Collection<UUID> tenantIds,
            @Param("now") java.time.Instant now);

    @Query(value = "select extversion from pg_extension where extname='unaccent'", nativeQuery = true)
    String findUnaccentExtensionVersion();

    @Query(value = """
        select procedure.proname from pg_proc procedure
        join pg_namespace namespace on namespace.oid=procedure.pronamespace
        where namespace.nspname='ai' and procedure.proname='immutable_unaccent'
        limit 1
        """, nativeQuery = true)
    String findImmutableUnaccentWrapper();
    @Query(value = """
        select chunk.chunk_id from ai.knowledge_chunks chunk
        join ai.knowledge_documents document on document.document_id=chunk.document_id
        join ai.knowledge_sources source on source.source_id=document.source_id
        where chunk.chunk_id in (:chunkIds)
          and chunk.status='READY' and document.status='READY' and source.status='ACTIVE'
          and chunk.document_version=document.document_version
          and chunk.access_scope in (:accessScopes) and document.access_scope in (:accessScopes)
          and source.access_scope in (:accessScopes)
          and (document.effective_from is null or document.effective_from<=now())
          and (document.effective_to is null or document.effective_to>now())
          and ((cast(:tenantId as uuid) is null and chunk.tenant_id is null and document.tenant_id is null and source.tenant_id is null)
            or (cast(:tenantId as uuid) is not null and (chunk.tenant_id is null or chunk.tenant_id=cast(:tenantId as uuid))
              and (document.tenant_id is null or document.tenant_id=cast(:tenantId as uuid))
              and (source.tenant_id is null or source.tenant_id=cast(:tenantId as uuid))))
          and document.document_version=(select max(d2.document_version) from ai.knowledge_documents d2
              where d2.document_key=document.document_key and d2.status='READY')
          and exists (select 1 from ai.knowledge_embeddings embedding where embedding.chunk_id=chunk.chunk_id
              and embedding.index_version_id=:indexVersionId and embedding.document_version=chunk.document_version)
        """, nativeQuery = true)
    List<UUID> findEligibleCachedChunkIds(@Param("tenantId") UUID tenantId, @Param("indexVersionId") UUID indexVersionId,
            @Param("accessScopes") List<String> accessScopes, @Param("chunkIds") List<UUID> chunkIds);

    interface SearchProjection {
        UUID getDocumentId(); UUID getChunkId(); String getTitle(); String getContent();
        String getSummary(); Integer getSourcePage(); String getSourceSection(); BigDecimal getScore();
    }
    interface CommonProjection extends SearchProjection {
        Integer getDocumentVersion(); java.time.Instant getReviewedAt(); String getDocumentChecksum();
    }

    String COMMON_SELECT = """
        select document.document_id as "documentId", chunk.chunk_id as "chunkId", chunk.title as "title",
          faq.content as "content", chunk.summary as "summary", faq.source_page as "sourcePage",
          faq.section as "sourceSection", document.document_version as "documentVersion",
          document.reviewed_at as "reviewedAt", document.checksum_sha256 as "documentChecksum",
        """;
    String COMMON_FROM = """
        from ai.knowledge_chunks chunk
        join ai.knowledge_documents document on document.document_id=chunk.document_id
        join ai.knowledge_sources source on source.source_id=document.source_id
        join lateral (
          select string_agg(block.content, chr(10) order by block.block_index) as content,
            min(coalesce(block.source_section,block.heading_path)) as section, min(block.source_page) as source_page
          from ai.knowledge_document_blocks block
          where block.document_id=document.document_id and block.doc_version=document.document_version
            and block.block_index between chunk.start_block_index and chunk.end_block_index
            and trim(regexp_replace(ai.immutable_unaccent(lower(coalesce(block.source_section,block.heading_path,''))),
                '^.*[>/]','')) ~ '^(faq|cau hoi.*thuong gap)$'
        ) faq on faq.content is not null
        where chunk.status='READY' and document.status='READY' and source.status='ACTIVE'
          and document.reviewed_at is not null and document.reviewed_by is not null
          and chunk.document_version=document.document_version
          and chunk.access_scope in (:accessScopes) and document.access_scope in (:accessScopes)
          and source.access_scope in (:accessScopes)
          and (document.effective_from is null or document.effective_from<=now())
          and (document.effective_to is null or document.effective_to>now())
          and ((cast(:tenantId as uuid) is null and chunk.tenant_id is null and document.tenant_id is null and source.tenant_id is null)
            or (cast(:tenantId as uuid) is not null and (chunk.tenant_id is null or chunk.tenant_id=cast(:tenantId as uuid))
              and (document.tenant_id is null or document.tenant_id=cast(:tenantId as uuid))
              and (source.tenant_id is null or source.tenant_id=cast(:tenantId as uuid))))
          and document.document_version=(select max(latest.document_version) from ai.knowledge_documents latest
              where latest.document_key=document.document_key and latest.status='READY')
        """;

    @Query(value = COMMON_SELECT + " ts_rank(chunk.search_vector_unaccent,to_tsquery('simple',:query)) as score " + COMMON_FROM + """
          and chunk.search_vector_unaccent @@ to_tsquery('simple',:query)
        order by score desc, document.document_id, chunk.chunk_id limit :limit
        """, nativeQuery = true)
    List<CommonProjection> commonFaqRows(@Param("tenantId") UUID tenantId, @Param("accessScopes") List<String> scopes,
            @Param("query") String query, @Param("limit") int limit);

    @Query(value = COMMON_SELECT + " cast(0 as numeric) as score " + COMMON_FROM + """
          and chunk.chunk_id in (:chunkIds)
        order by document.document_id, chunk.chunk_id
        for share of chunk,document,source
        """, nativeQuery = true)
    List<CommonProjection> lockCommonFaqRows(@Param("tenantId") UUID tenantId, @Param("accessScopes") List<String> scopes,
            @Param("chunkIds") List<UUID> chunkIds);

    interface HybridProjection {
        UUID getChunkId(); UUID getDocumentId(); String getTitle(); String getContent();
        String getSummary(); Integer getSourcePage(); String getSourceSection();
        Integer getVectorRank(); Integer getLexicalRank(); BigDecimal getVectorScore();
        BigDecimal getLexicalScore(); BigDecimal getFusedScore(); Integer getFinalRank();
    }
    interface EligibleChunkProjection {
        UUID getChunkId(); String getTitle(); String getContent(); String getSummary(); Integer getDocumentVersion();
    }
    interface ChunkSnapshotProjection {
        UUID getChunkId(); UUID getDocumentId(); Integer getDocumentVersion(); String getTitle();
        String getContent(); String getSummary(); Integer getSourcePage(); String getSourceSection();
        Integer getChunkIndex(); Integer getTokenCount(); String getHeadingPath();
    }

    default List<KnowledgeSearchResult> hybridSearch(UUID tenantId, String query,
            List<String> accessScopes, UUID indexVersionId, int limit) {
        return lexicalSearchRows(tenantId, query, accessScopes, indexVersionId, limit).stream()
                .map(KnowledgeChunkRepository::toSearchResult).toList();
    }

    @Query(value = """
        select document.document_id as "documentId", chunk.chunk_id as "chunkId",
               chunk.title as "title", chunk.content as "content", chunk.summary as "summary",
               chunk.source_page as "sourcePage", chunk.source_section as "sourceSection",
               ts_rank(chunk.search_vector, plainto_tsquery('simple', :query)) as "score"
        from ai.knowledge_chunks chunk
        join ai.knowledge_documents document on document.document_id=chunk.document_id
        join ai.knowledge_sources source on source.source_id=document.source_id
        where chunk.status='READY' and document.status='READY' and source.status='ACTIVE'
          and chunk.access_scope in (:accessScopes)
          and document.access_scope in (:accessScopes) and source.access_scope in (:accessScopes)
          and chunk.document_version=document.document_version
          and (document.effective_from is null or document.effective_from<=now())
          and (document.effective_to is null or document.effective_to>now())
          and ((cast(:tenantId as uuid) is null and document.tenant_id is null and source.tenant_id is null)
            or (cast(:tenantId as uuid) is not null and (document.tenant_id is null or document.tenant_id=cast(:tenantId as uuid))
              and (source.tenant_id is null or source.tenant_id=cast(:tenantId as uuid))))
          and ((cast(:tenantId as uuid) is null and chunk.tenant_id is null)
            or (cast(:tenantId as uuid) is not null and (chunk.tenant_id is null or chunk.tenant_id=cast(:tenantId as uuid))))
          and document.document_version=(select max(d2.document_version) from ai.knowledge_documents d2
              where d2.document_key=document.document_key and d2.status='READY')
          and chunk.search_vector @@ plainto_tsquery('simple', :query)
          and exists (select 1 from ai.knowledge_embeddings embedding
                      where embedding.chunk_id=chunk.chunk_id and embedding.index_version_id=:indexVersionId
                      and embedding.document_version=chunk.document_version)
        order by "score" desc, chunk.created_at desc limit :limit
        """, nativeQuery = true)
    List<SearchProjection> lexicalSearchRows(@Param("tenantId") UUID tenantId,
            @Param("query") String query, @Param("accessScopes") List<String> accessScopes,
            @Param("indexVersionId") UUID indexVersionId, @Param("limit") int limit);

    default List<KnowledgeSearchResult> vectorSearch(UUID tenantId, EmbeddingVector queryVector,
            List<String> accessScopes, UUID indexVersionId, int limit) {
        return vectorSearchRows(tenantId, KnowledgeEmbeddingRepository.toVectorLiteral(queryVector),
                accessScopes, indexVersionId, limit).stream()
                .map(KnowledgeChunkRepository::toSearchResult).toList();
    }

    @Query(value = """
        select document.document_id as "documentId", chunk.chunk_id as "chunkId",
               chunk.title as "title", chunk.content as "content", chunk.summary as "summary",
               chunk.source_page as "sourcePage", chunk.source_section as "sourceSection",
               (1-(embedding.embedding <=> cast(:queryVector as vector)))::numeric(8,5) as "score"
        from ai.knowledge_embeddings embedding
        join ai.knowledge_chunks chunk on chunk.chunk_id=embedding.chunk_id
        join ai.knowledge_documents document on document.document_id=chunk.document_id
        join ai.knowledge_sources source on source.source_id=document.source_id
        where embedding.index_version_id=:indexVersionId and embedding.document_version=chunk.document_version and chunk.status='READY'
          and document.status='READY' and source.status='ACTIVE'
          and chunk.access_scope in (:accessScopes)
          and document.access_scope in (:accessScopes) and source.access_scope in (:accessScopes)
          and chunk.document_version=document.document_version
          and (document.effective_from is null or document.effective_from<=now())
          and (document.effective_to is null or document.effective_to>now())
          and ((cast(:tenantId as uuid) is null and document.tenant_id is null and source.tenant_id is null)
            or (cast(:tenantId as uuid) is not null and (document.tenant_id is null or document.tenant_id=cast(:tenantId as uuid))
              and (source.tenant_id is null or source.tenant_id=cast(:tenantId as uuid))))
          and ((cast(:tenantId as uuid) is null and chunk.tenant_id is null)
            or (cast(:tenantId as uuid) is not null and (chunk.tenant_id is null or chunk.tenant_id=cast(:tenantId as uuid))))
          and document.document_version=(select max(d2.document_version) from ai.knowledge_documents d2
              where d2.document_key=document.document_key and d2.status='READY')
        order by embedding.embedding <=> cast(:queryVector as vector) limit :limit
        """, nativeQuery = true)
    List<SearchProjection> vectorSearchRows(@Param("tenantId") UUID tenantId,
            @Param("queryVector") String queryVector, @Param("accessScopes") List<String> accessScopes,
            @Param("indexVersionId") UUID indexVersionId, @Param("limit") int limit);

    default List<HybridSearchRow> hybridSearch(UUID tenantId, UUID indexVersionId,
            String queryVectorLiteral, String normalizedQuery, List<String> accessScopes,
            int vectorCandidateLimit, int lexicalCandidateLimit, double minimumVectorScore,
            double minimumLexicalScore, int rrfRankConstant, double weightVector,
            double weightLexical, int maxChunksPerDocument, int finalTopK) {
        return hybridSearchRows(tenantId, indexVersionId, queryVectorLiteral, normalizedQuery,
                accessScopes, vectorCandidateLimit, lexicalCandidateLimit, minimumVectorScore,
                minimumLexicalScore, rrfRankConstant, weightVector, weightLexical,
                maxChunksPerDocument, finalTopK).stream()
                .map(row -> new HybridSearchRow(row.getDocumentId(), row.getChunkId(), row.getTitle(),
                        row.getContent(), row.getSummary(), row.getSourcePage(), row.getSourceSection(),
                        row.getVectorRank(), row.getLexicalRank(), nonNull(row.getVectorScore()),
                        nonNull(row.getLexicalScore()), nonNull(row.getFusedScore()),
                        row.getFinalRank() == null ? 0 : row.getFinalRank()))
                .toList();
    }

    @Query(value = """
        with corpus as (
            select chunk.chunk_id, chunk.document_id, chunk.title, chunk.content, chunk.summary,
                   chunk.source_page, chunk.source_section
            from ai.knowledge_chunks chunk
            join ai.knowledge_documents document on document.document_id=chunk.document_id
            join ai.knowledge_sources source on source.source_id=document.source_id
            where chunk.status='READY' and document.status='READY' and source.status='ACTIVE'
              and chunk.access_scope in (:accessScopes)
          and document.access_scope in (:accessScopes) and source.access_scope in (:accessScopes)
          and chunk.document_version=document.document_version
          and (document.effective_from is null or document.effective_from<=now())
          and (document.effective_to is null or document.effective_to>now())
          and ((cast(:tenantId as uuid) is null and document.tenant_id is null and source.tenant_id is null)
            or (cast(:tenantId as uuid) is not null and (document.tenant_id is null or document.tenant_id=cast(:tenantId as uuid))
              and (source.tenant_id is null or source.tenant_id=cast(:tenantId as uuid))))
              and ((cast(:tenantId as uuid) is null and chunk.tenant_id is null)
                or (cast(:tenantId as uuid) is not null and (chunk.tenant_id is null or chunk.tenant_id=cast(:tenantId as uuid))))
              and (document.effective_from is null or document.effective_from<=now())
              and (document.effective_to is null or document.effective_to>now())
              and document.document_version=(select max(d2.document_version) from ai.knowledge_documents d2
                  where d2.document_key=document.document_key and d2.status='READY')
              and exists (select 1 from ai.knowledge_embeddings embedding
                  where embedding.chunk_id=chunk.chunk_id and embedding.index_version_id=:indexVersionId
                      and embedding.document_version=chunk.document_version)
        ), vector_branch as (
            select corpus.*,
                   (1-(embedding.embedding <=> cast(:queryVector as vector)))::numeric as vector_score,
                   row_number() over (order by embedding.embedding <=> cast(:queryVector as vector), corpus.chunk_id) as vector_rank
            from corpus join ai.knowledge_embeddings embedding
              on embedding.chunk_id=corpus.chunk_id and embedding.index_version_id=:indexVersionId
            where vector_norm(embedding.embedding)>0
              and (1-(embedding.embedding <=> cast(:queryVector as vector)))>=:minimumVectorScore
            order by embedding.embedding <=> cast(:queryVector as vector), corpus.chunk_id limit :vectorLimit
        ), lexical_branch as (
            select corpus.*,
                   ts_rank(corpus_search.search_vector_unaccent,
                     plainto_tsquery('simple', ai.immutable_unaccent(:normalizedQuery)))::numeric as lexical_score,
                   row_number() over (order by ts_rank(corpus_search.search_vector_unaccent,
                     plainto_tsquery('simple', ai.immutable_unaccent(:normalizedQuery))) desc, corpus.chunk_id) as lexical_rank
            from corpus join ai.knowledge_chunks corpus_search on corpus_search.chunk_id=corpus.chunk_id
            where corpus_search.search_vector_unaccent @@ plainto_tsquery('simple', ai.immutable_unaccent(:normalizedQuery))
              and ts_rank(corpus_search.search_vector_unaccent,
                    plainto_tsquery('simple', ai.immutable_unaccent(:normalizedQuery)))>=:minimumLexicalScore
            order by ts_rank(corpus_search.search_vector_unaccent,
                     plainto_tsquery('simple', ai.immutable_unaccent(:normalizedQuery))) desc, corpus.chunk_id
            limit :lexicalLimit
        ), fused as (
            select coalesce(v.chunk_id,l.chunk_id) as chunk_id,
                   coalesce(v.document_id,l.document_id) as document_id,
                   coalesce(v.title,l.title) as title, coalesce(v.content,l.content) as content,
                   coalesce(v.summary,l.summary) as summary, coalesce(v.source_page,l.source_page) as source_page,
                   coalesce(v.source_section,l.source_section) as source_section,
                   v.vector_rank, l.lexical_rank, v.vector_score, l.lexical_score,
                   (coalesce(:weightVector/(:rrfK+v.vector_rank),0)
                     +coalesce(:weightLexical/(:rrfK+l.lexical_rank),0))::numeric(12,8) as fused_score
            from vector_branch v full outer join lexical_branch l on l.chunk_id=v.chunk_id
        ), capped as (
            select fused.*, row_number() over (partition by document_id order by fused_score desc, chunk_id) as document_rank
            from fused
        )
        select chunk_id as "chunkId", document_id as "documentId", title as "title",
               content as "content", summary as "summary", source_page as "sourcePage",
               source_section as "sourceSection", vector_rank as "vectorRank",
               lexical_rank as "lexicalRank", vector_score as "vectorScore",
               lexical_score as "lexicalScore", fused_score as "fusedScore",
               row_number() over (order by fused_score desc, chunk_id) as "finalRank"
        from capped where document_rank<=:maxChunksPerDocument
        order by fused_score desc, chunk_id limit :finalTopK
        """, nativeQuery = true)
    List<HybridProjection> hybridSearchRows(@Param("tenantId") UUID tenantId,
            @Param("indexVersionId") UUID indexVersionId, @Param("queryVector") String queryVector,
            @Param("normalizedQuery") String normalizedQuery, @Param("accessScopes") List<String> accessScopes,
            @Param("vectorLimit") int vectorLimit, @Param("lexicalLimit") int lexicalLimit,
            @Param("minimumVectorScore") double minimumVectorScore,
            @Param("minimumLexicalScore") double minimumLexicalScore, @Param("rrfK") double rrfK,
            @Param("weightVector") double weightVector, @Param("weightLexical") double weightLexical,
            @Param("maxChunksPerDocument") int maxChunksPerDocument, @Param("finalTopK") int finalTopK);

    @Query(value = """
        select count(*) from ai.knowledge_chunks chunk
        join ai.knowledge_documents document on document.document_id=chunk.document_id
        join ai.knowledge_sources source on source.source_id=document.source_id
        where chunk.status='READY' and document.status='READY' and source.status='ACTIVE'
          and chunk.access_scope<>'TENANT_PRIVATE'
          and document.document_version=(select max(d2.document_version) from ai.knowledge_documents d2
              where d2.document_key=document.document_key and d2.status='READY')
          and not exists (select 1 from ai.knowledge_documents latest
              where latest.document_key=document.document_key and latest.status='ARCHIVED'
                and latest.document_version=(select max(all_versions.document_version)
                    from ai.knowledge_documents all_versions where all_versions.document_key=document.document_key))
        """, nativeQuery = true)
    long countEligibleChunks();

    @Query(value = """
        select md5(coalesce(string_agg(chunk.chunk_id::text||':'||chunk.document_version::text||':'||md5(chunk.content),
                                      '|' order by chunk.chunk_id),''))
        from ai.knowledge_chunks chunk
        join ai.knowledge_documents document on document.document_id=chunk.document_id
        join ai.knowledge_sources source on source.source_id=document.source_id
        where chunk.status='READY' and document.status='READY' and source.status='ACTIVE'
          and chunk.access_scope<>'TENANT_PRIVATE'
          and document.document_version=(select max(d2.document_version) from ai.knowledge_documents d2
              where d2.document_key=document.document_key and d2.status='READY')
          and not exists (select 1 from ai.knowledge_documents latest
              where latest.document_key=document.document_key and latest.status='ARCHIVED'
                and latest.document_version=(select max(all_versions.document_version)
                    from ai.knowledge_documents all_versions where all_versions.document_key=document.document_key))
        """, nativeQuery = true)
    String calculateEligibleCorpusChecksum();

    default List<KnowledgeChunk> findEligibleChunks(UUID indexVersionId, int limit) {
        return findEligibleChunkRows(indexVersionId, limit).stream()
                .map(row -> new KnowledgeChunk(row.getChunkId(), row.getTitle(), row.getContent(),
                        row.getSummary(), row.getDocumentVersion())).toList();
    }

    @Query(value = """
        select chunk.chunk_id as "chunkId", chunk.title as "title", chunk.content as "content",
               chunk.summary as "summary", chunk.document_version as "documentVersion"
        from ai.knowledge_chunks chunk
        join ai.knowledge_documents document on document.document_id=chunk.document_id
        join ai.knowledge_sources source on source.source_id=document.source_id
        where chunk.status='READY' and document.status='READY' and source.status='ACTIVE'
          and chunk.access_scope<>'TENANT_PRIVATE'
          and document.document_version=(select max(d2.document_version) from ai.knowledge_documents d2
              where d2.document_key=document.document_key and d2.status='READY')
          and not exists (select 1 from ai.knowledge_documents latest
              where latest.document_key=document.document_key and latest.status='ARCHIVED'
                and latest.document_version=(select max(all_versions.document_version)
                    from ai.knowledge_documents all_versions where all_versions.document_key=document.document_key))
          and not exists (select 1 from ai.knowledge_embeddings embedding
              where embedding.chunk_id=chunk.chunk_id and embedding.index_version_id=:indexVersionId
                      and embedding.document_version=chunk.document_version)
        order by chunk.created_at limit :limit
        """, nativeQuery = true)
    List<EligibleChunkProjection> findEligibleChunkRows(
            @Param("indexVersionId") UUID indexVersionId, @Param("limit") int limit);

    default void replaceChunksForDocument(UUID documentId, int documentVersion,
            String chunkerVersion, List<KnowledgeChunkDraft> chunks) {
        deleteByDocumentAndVersion(documentId, documentVersion);
        chunks.forEach(chunk -> insertDraft(documentId, documentVersion, chunkerVersion,
                chunk.getTitle(), chunk.getContent(), chunk.getSummary(), chunk.getSourcePage(),
                chunk.getSourceSection(), chunk.getChunkIndex(), chunk.getTokenCount(),
                chunk.getContentHash(), chunk.getHeadingPath(), chunk.getStartBlockIndex(),
                chunk.getEndBlockIndex()));
    }

    @Modifying
    @Query(value = "delete from ai.knowledge_chunks where document_id=:documentId and document_version=:documentVersion",
            nativeQuery = true)
    int deleteByDocumentAndVersion(@Param("documentId") UUID documentId,
            @Param("documentVersion") int documentVersion);

    @Modifying
    @Query(value = """
        insert into ai.knowledge_chunks (
            document_id, tenant_id, title, content, summary, source_page, source_section,
            chunk_index, access_scope, document_version, status, token_count, content_hash,
            heading_path, start_block_index, end_block_index, chunker_version, created_at
        ) select document.document_id, document.tenant_id, :title, :content, :summary,
                 :sourcePage, :sourceSection, :chunkIndex, document.access_scope, :documentVersion,
                 'DRAFT', :tokenCount, :contentHash, :headingPath, :startBlockIndex,
                 :endBlockIndex, :chunkerVersion, now()
          from ai.knowledge_documents document where document.document_id=:documentId
        """, nativeQuery = true)
    int insertDraft(@Param("documentId") UUID documentId, @Param("documentVersion") int documentVersion,
            @Param("chunkerVersion") String chunkerVersion, @Param("title") String title,
            @Param("content") String content, @Param("summary") String summary,
            @Param("sourcePage") Integer sourcePage, @Param("sourceSection") String sourceSection,
            @Param("chunkIndex") int chunkIndex, @Param("tokenCount") Integer tokenCount,
            @Param("contentHash") String contentHash, @Param("headingPath") String headingPath,
            @Param("startBlockIndex") Integer startBlockIndex, @Param("endBlockIndex") Integer endBlockIndex);

    default List<KnowledgeChunkSnapshot> findByDocumentIdAndDocVersion(UUID documentId, int documentVersion) {
        return findSnapshotRows(documentId, documentVersion).stream()
                .map(row -> KnowledgeChunkSnapshot.of(row.getChunkId(), row.getDocumentId(),
                        row.getDocumentVersion(), row.getTitle(), row.getContent(), row.getSummary(),
                        row.getSourcePage(), row.getSourceSection(), row.getChunkIndex(),
                        row.getTokenCount(), row.getHeadingPath())).toList();
    }

    @Query(value = """
        select chunk_id as "chunkId", document_id as "documentId", document_version as "documentVersion",
               title as "title", content as "content", summary as "summary", source_page as "sourcePage",
               source_section as "sourceSection", chunk_index as "chunkIndex", token_count as "tokenCount",
               heading_path as "headingPath"
        from ai.knowledge_chunks where document_id=:documentId and document_version=:documentVersion
        order by chunk_index
        """, nativeQuery = true)
    List<ChunkSnapshotProjection> findSnapshotRows(@Param("documentId") UUID documentId,
            @Param("documentVersion") int documentVersion);

    @Modifying
    @Query(value = """
        update ai.knowledge_chunks set status=:toStatus, updated_at=now()
        where document_id=:documentId and status=:fromStatus
        """, nativeQuery = true)
    int updateStatusByDocumentId(@Param("documentId") UUID documentId,
            @Param("fromStatus") String fromStatus, @Param("toStatus") String toStatus);

    @Query(value = "select count(*) from ai.knowledge_chunks where document_id=:documentId", nativeQuery = true)
    long countByDocumentId(@Param("documentId") UUID documentId);

    private static KnowledgeSearchResult toSearchResult(SearchProjection row) {
        return new KnowledgeSearchResult(row.getDocumentId(), row.getChunkId(), row.getTitle(),
                row.getContent(), row.getSummary(), row.getSourcePage(), row.getSourceSection(),
                nonNull(row.getScore()));
    }

    private static BigDecimal nonNull(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
