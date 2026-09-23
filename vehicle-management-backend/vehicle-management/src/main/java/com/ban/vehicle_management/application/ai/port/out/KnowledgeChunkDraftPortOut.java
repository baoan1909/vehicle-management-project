package com.ban.vehicle_management.application.ai.port.out;

import com.ban.vehicle_management.domain.ai.knowledge.model.KnowledgeChunkDraft;
import com.ban.vehicle_management.domain.ai.knowledge.model.KnowledgeChunkSnapshot;
import com.ban.vehicle_management.shared.enumeration.ai.KnowledgeChunkStatus;
import java.util.List;
import java.util.UUID;

/**
 * Chunk persistence during ingestion. Replacing chunks for a document version is
 * idempotent: previous chunks of that version are removed and the new ones inserted,
 * which keeps re-extraction and retries safe at the expense of only touching that
 * version's rows.
 */
public interface KnowledgeChunkDraftPortOut {

    void replaceChunksForDocument(UUID documentId, int documentVersion, String chunkerVersion, List<KnowledgeChunkDraft> chunks);

    List<KnowledgeChunkSnapshot> findByDocumentIdAndDocVersion(UUID documentId, int documentVersion);

    void updateStatusByDocumentId(UUID documentId, KnowledgeChunkStatus from, KnowledgeChunkStatus to);

    long countByDocumentId(UUID documentId);
}