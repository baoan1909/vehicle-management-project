package com.ban.vehicle_management.entrypoint.dto.ai.knowledge.response;

import java.time.Instant;

import com.ban.vehicle_management.shared.enumeration.ai.IngestionStage;
import com.ban.vehicle_management.shared.enumeration.ai.KnowledgeIngestionJobStatus;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class KnowledgeIngestionJobResponse {
    private UUID ingestionJobId;
    private UUID documentId;
    private KnowledgeIngestionJobStatus status;
    private IngestionStage currentStage;
    private Integer progressPercent;
    private Integer attemptCount;
    private Integer maxAttempts;
    private IngestionStage lastCompletedStage;
    private Instant startedAt;
    private Instant completedAt;
    private String errorCode;
    private String errorMessageRedacted;
    private Instant nextAttemptAt;
    private Instant createdAt;
    private Instant updatedAt;
}
