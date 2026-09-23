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
@Table(name = "ai_retrieval_audit_results", schema = "ai")
@Getter
@Setter
public class RetrievalAuditResultEntity {

    @Id
    @Column(name = "audit_result_id", nullable = false)
    private UUID auditResultId;

    @Column(name = "retrieval_audit_id", nullable = false)
    private UUID retrievalAuditId;

    @Column(name = "document_id", nullable = false)
    private UUID documentId;

    @Column(name = "chunk_id", nullable = false)
    private UUID chunkId;

    @Column(name = "vector_rank")
    private Integer vectorRank;

    @Column(name = "lexical_rank")
    private Integer lexicalRank;

    @Column(name = "vector_score")
    private BigDecimal vectorScore;

    @Column(name = "lexical_score")
    private BigDecimal lexicalScore;

    @Column(name = "fused_score")
    private BigDecimal fusedScore;

    @Column(name = "final_rank")
    private Integer finalRank;

    @Column(name = "selected_for_context", nullable = false)
    private Boolean selectedForContext;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
