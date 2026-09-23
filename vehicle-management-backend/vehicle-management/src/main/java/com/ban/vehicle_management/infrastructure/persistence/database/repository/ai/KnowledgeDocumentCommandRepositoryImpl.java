package com.ban.vehicle_management.infrastructure.persistence.database.repository.ai;

import com.ban.vehicle_management.infrastructure.persistence.database.entity.ai.KnowledgeDocumentEntity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class KnowledgeDocumentCommandRepositoryImpl implements KnowledgeDocumentCommandRepository {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    @Transactional
    public Optional<UUID> insertIfAbsent(KnowledgeDocumentEntity entity) {
        Query nativeQuery = entityManager.createNativeQuery("""
                INSERT INTO ai.knowledge_documents (
                    document_id,
                    source_id,
                    document_key,
                    supersedes_document_id,
                    tenant_id,
                    title,
                    object_key,
                    original_filename,
                    file_extension,
                    mime_type,
                    file_size_bytes,
                    checksum_sha256,
                    document_version,
                    access_scope,
                    status,
                    failure_code,
                    effective_from,
                    created_at,
                    updated_at
                ) VALUES (
                    :documentId,
                    :sourceId,
                    :documentKey,
                    :supersedesDocumentId,
                    :tenantId,
                    :title,
                    :objectKey,
                    :originalFilename,
                    :fileExtension,
                    :mimeType,
                    :fileSizeBytes,
                    :checksumSha256,
                    :documentVersion,
                    :accessScope,
                    :status,
                    :failureCode,
                    :effectiveFrom,
                    :createdAt,
                    :updatedAt
                )
                ON CONFLICT (source_id, checksum_sha256, document_version)
                WHERE checksum_sha256 IS NOT NULL
                DO NOTHING
                RETURNING document_id
                """)
                .setParameter("documentId", entity.getDocumentId())
                .setParameter("sourceId", entity.getSourceId())
                .setParameter("documentKey", entity.getDocumentKey())
                .setParameter("supersedesDocumentId", entity.getSupersedesDocumentId())
                .setParameter("tenantId", entity.getTenantId())
                .setParameter("title", entity.getTitle())
                .setParameter("objectKey", entity.getObjectKey())
                .setParameter("originalFilename", entity.getOriginalFilename())
                .setParameter("fileExtension", entity.getFileExtension())
                .setParameter("mimeType", entity.getMimeType())
                .setParameter("fileSizeBytes", entity.getFileSizeBytes())
                .setParameter("checksumSha256", entity.getChecksumSha256())
                .setParameter("documentVersion", entity.getDocumentVersion())
                .setParameter("accessScope", entity.getAccessScope().name())
                .setParameter("status", entity.getStatus().name())
                .setParameter("failureCode", entity.getFailureCode())
                .setParameter("effectiveFrom", entity.getEffectiveFrom())
                .setParameter("createdAt", entity.getCreatedAt())
                .setParameter("updatedAt", entity.getUpdatedAt());

        java.util.List<?> result = nativeQuery.getResultList();

        if (result == null || result.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of((UUID) result.get(0));
    }

    @Override
    @Transactional
    public void lockDocumentKey(UUID documentKey) {
        entityManager.createNativeQuery(
                        "SELECT pg_advisory_xact_lock(hashtextextended(CAST(:documentKey AS text), 0))")
                .setParameter("documentKey", documentKey)
                .getSingleResult();
    }
}
