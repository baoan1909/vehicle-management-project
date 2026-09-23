package com.ban.vehicle_management.application.ai.port.out;

import com.ban.vehicle_management.application.ai.query.KnowledgeDocumentQuery;
import com.ban.vehicle_management.domain.ai.knowledge.model.KnowledgeDocument;
import com.ban.vehicle_management.shared.enumeration.ai.KnowledgeDocumentStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Persistence for uploaded knowledge documents. Source+checksum uniqueness is enforced
 * at the database level; uploads translate the conflict into idempotency semantics.
 */
public interface KnowledgeDocumentPortOut {

    KnowledgeDocument save(KnowledgeDocument document);

    /**
     * Inserts the document and returns the created row, or returns the existing row when
     * the same source already has a document with the same content checksum.
     */
    KnowledgeDocument insertIdempotent(KnowledgeDocument document);

    Optional<KnowledgeDocument> findById(UUID documentId);

    Optional<KnowledgeDocument> findByIdForUpdate(UUID documentId);

    Optional<KnowledgeDocument> findLatestByDocumentKey(UUID sourceId, UUID documentKey);

    Optional<KnowledgeDocument> findBySourceIdAndChecksum(UUID sourceId, String checksumSha256);

    List<KnowledgeDocument> findBySourceId(UUID sourceId);

    List<KnowledgeDocument> findAll();

    Page<KnowledgeDocument> findAll(KnowledgeDocumentQuery query, Pageable pageable);

    /**
     * Takes a transaction-scoped advisory lock for the logical document key and returns
     * its latest version, including versions that are still processing or failed.
     * Used when creating a new reindex version to ensure correct version sequencing.
     */
    Optional<LockedDocumentVersion> lockLatestVersionByDocumentKey(UUID documentKey);

    record LockedDocumentVersion(UUID documentId, int maxVersion, KnowledgeDocumentStatus status) {}
}
