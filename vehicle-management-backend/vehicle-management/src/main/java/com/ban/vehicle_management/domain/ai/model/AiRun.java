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
    private AiProvider provider;
    private String modelId;
    private String promptVersion;
    private AiRunStatus status;
    private Long latencyMs;
    private Integer inputTokens;
    private Integer outputTokens;
    private String failureCode;
    private Instant createdAt;
}
