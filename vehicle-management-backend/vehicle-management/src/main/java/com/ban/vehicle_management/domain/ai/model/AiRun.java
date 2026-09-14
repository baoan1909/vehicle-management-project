package com.ban.vehicle_management.domain.ai.model;

import com.ban.vehicle_management.shared.enumeration.ai.AiProvider;
import com.ban.vehicle_management.shared.enumeration.ai.AiRunStatus;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AiRun {

    private UUID runId;
    private UUID conversationId;
    private UUID inputMessageId;
    private UUID outputMessageId;
    private UUID configurationId;
    private AiProvider provider;
    private String modelId;
    private String promptVersion;
    private Integer rolloutVersion;
    private Integer attemptNumber;
    private AiRunStatus status;
    private Long latencyMs;
    private Integer inputTokens;
    private Integer outputTokens;
    private String failureCode;
    private Integer providerStatus;
    private String providerErrorCode;
    private String providerErrorMessageRedacted;
    private String fieldViolationsRedacted = "[]";
    private Boolean failureRetryable;
    private Instant createdAt;
}
