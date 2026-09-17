package com.ban.vehicle_management.infrastructure.persistence.database.repository.ai;

import com.ban.vehicle_management.infrastructure.persistence.database.entity.ai.KnowledgeDocumentEntity;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface KnowledgeDocumentRepository extends JpaRepository<KnowledgeDocumentEntity, UUID>, JpaSpecificationExecutor<KnowledgeDocumentEntity> {

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

}
