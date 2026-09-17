package com.ban.vehicle_management.domain.ai.model;

import com.ban.vehicle_management.shared.enumeration.ai.AiModelStatus;
import com.ban.vehicle_management.shared.enumeration.ai.AiProvider;
import com.ban.vehicle_management.shared.enumeration.ai.AiUseCase;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AiModelConfiguration {

    private UUID configurationId;
    private AiProvider provider;
    private AiUseCase useCase;
    private String modelId;
    private String apiVersion;
    private BigDecimal temperature;
    private Integer maxOutputTokens;
    private Integer outputDimension;
    private Integer priority;
    private Integer rolloutPercentage;
    private AiModelStatus status;
    private boolean requiresFunctionCalling;
    private boolean requiresStructuredOutput;
    private boolean freeTierApproved;
    private Instant createdAt;
    private UUID createdBy;
    private Instant updatedAt;
    private UUID updatedBy;
}
