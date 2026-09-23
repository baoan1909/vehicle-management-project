package com.ban.vehicle_management.domain.ai.model;

import com.ban.vehicle_management.shared.enumeration.ai.AiToolCallStatus;
import com.ban.vehicle_management.shared.enumeration.ai.AiToolType;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AiToolCall {

    private UUID toolCallId;
    private UUID runId;
    private UUID conversationId;
    private UUID inputMessageId;
    private UUID requestedBy;
    private String toolName;
    private AiToolType toolType;
    private String requestPayloadRedacted;
    private String argumentPayloadRedacted;
    private String responsePayloadRedacted;
    private AiToolCallStatus status;
    private String idempotencyKey;
    private UUID confirmedBy;
    private Instant confirmedAt;
    private Instant executedAt;
    private Instant expiresAt;
    private String failureCode;
    private Integer version;
    private Instant createdAt;
    private Instant updatedAt;

    public boolean awaitingConfirmationAt(Instant now) {
        return status == AiToolCallStatus.AWAITING_CONFIRMATION
                && expiresAt != null
                && now != null
                && now.isBefore(expiresAt);
    }
}
