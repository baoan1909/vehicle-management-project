package com.ban.vehicle_management.infrastructure.persistence.database.entity.ai;

import com.ban.vehicle_management.shared.enumeration.ai.AiProvider;
import com.ban.vehicle_management.shared.enumeration.ai.AiRunStatus;
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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "ai_runs", schema = "ai")
@Getter
@Setter
public class AiRunEntity {

    @Id
    @Column(name = "run_id", nullable = false)
    private UUID runId;

    @Column(name = "conversation_id", nullable = false)
    private UUID conversationId;

    @Column(name = "input_message_id", nullable = false)
    private UUID inputMessageId;

    @Column(name = "output_message_id")
    private UUID outputMessageId;

    @Column(name = "configuration_id")
    private UUID configurationId;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider", nullable = false)
    private AiProvider provider;

    @Column(name = "model_id", nullable = false)
    private String modelId;

    @Column(name = "prompt_version", nullable = false)
    private String promptVersion;

    @Column(name = "rollout_version", nullable = false)
    private Integer rolloutVersion;

    @Column(name = "attempt_number", nullable = false)
    private Integer attemptNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private AiRunStatus status;

    @Column(name = "latency_ms")
    private Long latencyMs;

    @Column(name = "input_tokens")
    private Integer inputTokens;

    @Column(name = "output_tokens")
    private Integer outputTokens;

    @Column(name = "failure_code")
    private String failureCode;

    @Column(name = "provider_status")
    private Integer providerStatus;

    @Column(name = "provider_error_code")
    private String providerErrorCode;

    @Column(name = "provider_error_message_redacted")
    private String providerErrorMessageRedacted;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "field_violations_redacted", columnDefinition = "jsonb")
    private String fieldViolationsRedacted;

    @Column(name = "failure_retryable")
    private Boolean failureRetryable;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
