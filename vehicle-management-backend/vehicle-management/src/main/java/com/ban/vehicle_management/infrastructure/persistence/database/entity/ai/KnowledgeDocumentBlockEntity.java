package com.ban.vehicle_management.infrastructure.persistence.database.entity.ai;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "knowledge_document_blocks", schema = "ai")
@Getter
@Setter
public class KnowledgeDocumentBlockEntity {

    @Id
    @Column(name = "block_id", nullable = false)
    private UUID blockId;

    @Column(name = "document_id", nullable = false)
    private UUID documentId;

    @Column(name = "doc_version", nullable = false)
    private Integer docVersion;

    @Column(name = "block_index", nullable = false)
    private Integer blockIndex;

    @Column(name = "kind", nullable = false)
    private String kind;

    @Column(name = "heading_path")
    private String headingPath;

    @Column(name = "source_page")
    private Integer sourcePage;

    @Column(name = "source_section")
    private String sourceSection;

    @Column(name = "content", nullable = false)
    private String content;

    @Column(name = "hash")
    private String hash;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}