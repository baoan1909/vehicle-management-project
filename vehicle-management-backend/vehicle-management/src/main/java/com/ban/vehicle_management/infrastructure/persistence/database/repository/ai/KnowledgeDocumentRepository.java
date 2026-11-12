package com.ban.vehicle_management.infrastructure.persistence.database.repository.ai;

import com.ban.vehicle_management.infrastructure.persistence.database.entity.ai.KnowledgeDocumentEntity;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.time.Instant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Modifying;
import java.util.Collection;

public interface KnowledgeDocumentRepository extends JpaRepository<KnowledgeDocumentEntity, UUID>, JpaSpecificationExecutor<KnowledgeDocumentEntity> {

    default Optional<UUID> insertIfAbsent(KnowledgeDocumentEntity entity) {
        List<UUID> ids = insertIfAbsentRaw(
                entity.getDocumentId(), entity.getSourceId(), entity.getDocumentKey(),
                entity.getSupersedesDocumentId(), entity.getTenantId(), entity.getTitle(),
                entity.getObjectKey(), entity.getOriginalFilename(), entity.getFileExtension(),
                entity.getMimeType(), entity.getFileSizeBytes(), entity.getChecksumSha256(),
                entity.getDocumentVersion(), entity.getAccessScope().name(), entity.getStatus().name(),
                entity.getFailureCode(), entity.getEffectiveFrom(), entity.getCreatedAt(), entity.getUpdatedAt());
        return ids.stream().findFirst();
    }

    @Query(value = """
        insert into ai.knowledge_documents (
            document_id, source_id, document_key, supersedes_document_id, tenant_id,
            title, object_key, original_filename, file_extension, mime_type,
            file_size_bytes, checksum_sha256, document_version, access_scope, status,
            failure_code, effective_from, created_at, updated_at
        ) values (
            :documentId, :sourceId, :documentKey, :supersedesDocumentId, :tenantId,
            :title, :objectKey, :originalFilename, :fileExtension, :mimeType,
            :fileSizeBytes, :checksumSha256, :documentVersion, :accessScope, :status,
            :failureCode, :effectiveFrom, :createdAt, :updatedAt
        ) on conflict (source_id, checksum_sha256, document_version)
          where checksum_sha256 is not null
        do nothing returning document_id
        """, nativeQuery = true)
    List<UUID> insertIfAbsentRaw(
            @Param("documentId") UUID documentId, @Param("sourceId") UUID sourceId,
            @Param("documentKey") UUID documentKey, @Param("supersedesDocumentId") UUID supersedesDocumentId,
            @Param("tenantId") UUID tenantId, @Param("title") String title,
            @Param("objectKey") String objectKey, @Param("originalFilename") String originalFilename,
            @Param("fileExtension") String fileExtension, @Param("mimeType") String mimeType,
            @Param("fileSizeBytes") Long fileSizeBytes, @Param("checksumSha256") String checksumSha256,
            @Param("documentVersion") int documentVersion, @Param("accessScope") String accessScope,
            @Param("status") String status, @Param("failureCode") String failureCode,
            @Param("effectiveFrom") Instant effectiveFrom, @Param("createdAt") Instant createdAt,
            @Param("updatedAt") Instant updatedAt);

    @Query(value = "select pg_advisory_xact_lock(hashtextextended(cast(:documentKey as text), 0))",
            nativeQuery = true)
    Long lockDocumentKey(@Param("documentKey") UUID documentKey);

    List<KnowledgeDocumentEntity> findBySourceIdOrderByCreatedAtDesc(UUID sourceId);

    Optional<KnowledgeDocumentEntity> findFirstBySourceIdAndDocumentKeyOrderByDocumentVersionDesc(
            UUID sourceId, UUID documentKey);

    Optional<KnowledgeDocumentEntity> findFirstByDocumentKeyOrderByDocumentVersionDesc(UUID documentKey);

    Optional<KnowledgeDocumentEntity> findFirstBySourceIdAndChecksumSha256OrderByCreatedAtDesc(
            UUID sourceId, String checksumSha256);

    Optional<KnowledgeDocumentEntity> findBySourceIdAndChecksumSha256AndDocumentVersion(
            UUID sourceId, String checksumSha256, int documentVersion);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT document FROM KnowledgeDocumentEntity document WHERE document.documentId = :documentId")
    Optional<KnowledgeDocumentEntity> findByIdForUpdate(@Param("documentId") UUID documentId);

    @Query(value = """
        select object_key from ai.knowledge_documents
         where tenant_id in (:tenantIds) and access_scope = 'TENANT_PRIVATE'
           and object_key is not null and status <> 'ARCHIVED'
        """, nativeQuery = true)
    List<String> findTenantPrivateObjectKeys(@Param("tenantIds") Collection<UUID> tenantIds);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
        update ai.knowledge_documents
           set status = 'ARCHIVED', archived_at = :now, archived_by = :actorAccountId,
               effective_to = coalesce(effective_to, :now), updated_at = :now
         where tenant_id in (:tenantIds) and access_scope = 'TENANT_PRIVATE' and status <> 'ARCHIVED'
        """, nativeQuery = true)
    int archiveTenantPrivate(
            @Param("tenantIds") Collection<UUID> tenantIds,
            @Param("actorAccountId") UUID actorAccountId,
            @Param("now") Instant now);

}
