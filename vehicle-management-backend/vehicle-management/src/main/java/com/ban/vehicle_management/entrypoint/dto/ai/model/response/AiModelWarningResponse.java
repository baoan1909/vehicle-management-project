package com.ban.vehicle_management.entrypoint.dto.ai.model.response;

import com.ban.vehicle_management.shared.enumeration.ai.AiModelWarningSeverity;
import com.ban.vehicle_management.shared.enumeration.ai.AiModelWarningStatus;
import com.ban.vehicle_management.shared.enumeration.ai.AiProvider;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AiModelWarningResponse {
    private UUID warningId;
    private AiProvider provider;
    private String modelId;
    private UUID configurationId;
    private String warningCode;
    private AiModelWarningSeverity severity;
    private AiModelWarningStatus status;
    private String detail;
    private String detectedAt;
    private String resolvedAt;
}
