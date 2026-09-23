package com.ban.vehicle_management.infrastructure.persistence.database.entity.ai;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "ai_message_citations", schema = "ai")
@Getter
@Setter
public class AiMessageCitationEntity {

    @Id
    @Column(name = "citation_id", nullable = false)
    private UUID citationId;

    @Column(name = "message_id", nullable = false)
    private UUID messageId;

    @Column(name = "document_id", nullable = false)
    private UUID documentId;

    @Column(name = "chunk_id", nullable = false)
    private UUID chunkId;

    @Column(name = "label", nullable = false, length = 20)
    private String label;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "source_page")
    private Integer sourcePage;

    @Column(name = "source_section")
    private String sourceSection;

    @Column(name = "retrieval_score")
    private BigDecimal retrievalScore;

    @Column(name = "retrieval_audit_id")
    private UUID retrievalAuditId;

    @Column(name = "index_version_id")
    private UUID indexVersionId;

    @Column(name = "citation_order", nullable = false)
    private Integer citationOrder;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
