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
@Table(name = "ai_retrieval_audits", schema = "ai")
@Getter
@Setter
public class RetrievalAuditEntity {

    @Id
    @Column(name = "retrieval_audit_id", nullable = false)
    private UUID retrievalAuditId;

    @Column(name = "run_id")
    private UUID runId;

    @Column(name = "conversation_id")
    private UUID conversationId;

    @Column(name = "input_message_id")
    private UUID inputMessageId;

    @Column(name = "output_message_id")
    private UUID outputMessageId;

    @Column(name = "requested_by")
    private UUID requestedBy;

    @Column(name = "tenant_id")
    private UUID tenantId;

    @Column(name = "query_redacted", nullable = false)
    private String queryRedacted;

    @Column(name = "normalized_query_hash")
    private String normalizedQueryHash;

    @Column(name = "active_index_version_id")
    private UUID activeIndexVersionId;

    @Column(name = "retrieval_policy_version")
    private String retrievalPolicyVersion;

    @Column(name = "threshold_version")
    private String thresholdVersion;

    @Column(name = "access_scopes")
    private String accessScopes;

    @Column(name = "result_count", nullable = false)
    private Integer resultCount;

    @Column(name = "vector_candidate_count", nullable = false)
    private Integer vectorCandidateCount;

    @Column(name = "lexical_candidate_count", nullable = false)
    private Integer lexicalCandidateCount;

    @Column(name = "fused_result_count", nullable = false)
    private Integer fusedResultCount;

    @Column(name = "final_result_count", nullable = false)
    private Integer finalResultCount;

    @Column(name = "top_k", nullable = false)
    private Integer topK;

    @Column(name = "max_chunks_per_document", nullable = false)
    private Integer maxChunksPerDocument;

    @Column(name = "grounded_confidence")
    private BigDecimal groundedConfidence;

    @Column(name = "diagnostic_code")
    private String diagnosticCode;

    @Column(name = "latency_ms")
    private Long latencyMs;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
