package com.ban.vehicle_management.domain.ai.model;

import com.ban.vehicle_management.shared.enumeration.ai.AiModelWarningSeverity;
import com.ban.vehicle_management.shared.enumeration.ai.AiModelWarningStatus;
import com.ban.vehicle_management.shared.enumeration.ai.AiProvider;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AiModelWarning {

    private UUID warningId;
    private AiProvider provider;
    private String modelId;
    private UUID configurationId;
    private String warningCode;
    private AiModelWarningSeverity severity;
    private AiModelWarningStatus status;
    private String detail;
    private Instant detectedAt;
    private Instant resolvedAt;
    private Instant createdAt;
    private Instant updatedAt;
}
