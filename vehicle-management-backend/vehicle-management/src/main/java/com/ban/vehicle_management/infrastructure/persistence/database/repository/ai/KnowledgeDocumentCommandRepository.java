package com.ban.vehicle_management.infrastructure.persistence.database.repository.ai;

import com.ban.vehicle_management.infrastructure.persistence.database.entity.ai.KnowledgeDocumentEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

/**
 * Command repository for KnowledgeDocument - handles PostgreSQL-specific operations
 * like INSERT ... ON CONFLICT ... RETURNING that cannot be expressed in standard JPA.
 */
public interface KnowledgeDocumentCommandRepository {

    /**
     * Inserts a document and returns its ID if successful, or empty if conflict on
     * (source_id, checksum_sha256, document_version).
     * Uses native SQL to achieve atomic upsert with RETURNING.
     */
    Optional<UUID> insertIfAbsent(KnowledgeDocumentEntity entity);

    void lockDocumentKey(UUID documentKey);
}
