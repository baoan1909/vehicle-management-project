package com.ban.vehicle_management.infrastructure.persistence.database.entity.ai;

import com.ban.vehicle_management.shared.enumeration.ai.AiToolCallStatus;
import com.ban.vehicle_management.shared.enumeration.ai.AiToolType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "ai_tool_calls", schema = "ai")
@Getter
@Setter
public class AiToolCallEntity {

    @Id
    @Column(name = "tool_call_id", nullable = false)
    private UUID toolCallId;

    @Column(name = "run_id", nullable = false)
    private UUID runId;

    @Column(name = "conversation_id")
    private UUID conversationId;

    @Column(name = "input_message_id")
    private UUID inputMessageId;

    @Column(name = "requested_by")
    private UUID requestedBy;

    @Column(name = "tool_name", nullable = false)
    private String toolName;

    @Enumerated(EnumType.STRING)
    @Column(name = "tool_type", nullable = false)
    private AiToolType toolType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "request_payload_redacted", nullable = false, columnDefinition = "jsonb")
    private String requestPayloadRedacted;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "argument_payload_redacted", nullable = false, columnDefinition = "jsonb")
    private String argumentPayloadRedacted;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "response_payload_redacted", nullable = false, columnDefinition = "jsonb")
    private String responsePayloadRedacted;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private AiToolCallStatus status;

    @Column(name = "idempotency_key")
    private String idempotencyKey;

    @Column(name = "confirmed_by")
    private UUID confirmedBy;

    @Column(name = "confirmed_at")
    private Instant confirmedAt;

    @Column(name = "executed_at")
    private Instant executedAt;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "failure_code")
    private String failureCode;

    @Version
    @Column(name = "version", nullable = false)
    private Integer version;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;
}
