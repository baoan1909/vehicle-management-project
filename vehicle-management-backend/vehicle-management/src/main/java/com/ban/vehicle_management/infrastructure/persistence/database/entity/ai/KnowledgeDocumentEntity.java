package com.ban.vehicle_management.infrastructure.persistence.database.entity.ai;

import com.ban.vehicle_management.shared.enumeration.ai.KnowledgeAccessScope;
import com.ban.vehicle_management.shared.enumeration.ai.KnowledgeDocumentStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "knowledge_documents", schema = "ai")
@Getter
@Setter
public class KnowledgeDocumentEntity {

    @Id
    @Column(name = "document_id", nullable = false)
    private UUID documentId;

    @Column(name = "source_id", nullable = false)
    private UUID sourceId;

    @Column(name = "document_key", nullable = false)
    private UUID documentKey;

    @Column(name = "supersedes_document_id")
    private UUID supersedesDocumentId;

    @Column(name = "tenant_id")
    private UUID tenantId;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "object_key")
    private String objectKey;

    @Column(name = "original_filename")
    private String originalFilename;

    @Column(name = "file_extension")
    private String fileExtension;

    @Column(name = "mime_type")
    private String mimeType;

    @Column(name = "file_size_bytes")
    private Long fileSizeBytes;

    @Column(name = "checksum_sha256")
    private String checksumSha256;

    @Column(name = "document_version", nullable = false)
    private Integer documentVersion;

    @Enumerated(EnumType.STRING)
    @Column(name = "access_scope", nullable = false)
    private KnowledgeAccessScope accessScope;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private KnowledgeDocumentStatus status;

    @Column(name = "failure_code")
    private String failureCode;

    @Column(name = "effective_from")
    private Instant effectiveFrom;

    @Column(name = "effective_to")
    private Instant effectiveTo;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    @Column(name = "reviewed_by")
    private UUID reviewedBy;

    @Column(name = "archived_at")
    private Instant archivedAt;

    @Column(name = "archived_by")
    private UUID archivedBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;
}