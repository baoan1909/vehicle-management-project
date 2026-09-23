package com.ban.vehicle_management.domain.ai.model;

import com.ban.vehicle_management.shared.enumeration.ai.AssistantJobStatus;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AssistantJob {

    private UUID jobId;
    private UUID inputMessageId;
    private UUID conversationId;
    private AssistantJobStatus status;
    private Integer attemptCount;
    private Instant nextAttemptAt;
    private Instant lockedAt;
    private String lockedBy;
    private Instant lockExpiresAt;
    private String errorCode;
    private Instant createdAt;
    private Instant updatedAt;
}
