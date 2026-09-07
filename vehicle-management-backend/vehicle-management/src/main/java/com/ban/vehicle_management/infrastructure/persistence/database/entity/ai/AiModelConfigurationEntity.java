package com.ban.vehicle_management.infrastructure.persistence.database.entity.ai;

import com.ban.vehicle_management.shared.enumeration.ai.AiModelStatus;
import com.ban.vehicle_management.shared.enumeration.ai.AiProvider;
import com.ban.vehicle_management.shared.enumeration.ai.AiUseCase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "ai_model_configurations", schema = "ai")
@Getter
@Setter
public class AiModelConfigurationEntity {

    @Id
    @Column(name = "configuration_id", nullable = false)
    private UUID configurationId;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider", nullable = false)
    private AiProvider provider;

    @Enumerated(EnumType.STRING)
    @Column(name = "use_case", nullable = false)
    private AiUseCase useCase;

    @Column(name = "model_id", nullable = false)
    private String modelId;

    @Column(name = "api_version")
    private String apiVersion;

    @Column(name = "temperature", nullable = false)
    private BigDecimal temperature;

    @Column(name = "max_output_tokens", nullable = false)
    private Integer maxOutputTokens;

    @Column(name = "priority", nullable = false)
    private Integer priority;

    @Column(name = "rollout_percentage", nullable = false)
    private Integer rolloutPercentage;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private AiModelStatus status;

    @Column(name = "requires_function_calling", nullable = false)
    private boolean requiresFunctionCalling;

    @Column(name = "requires_structured_output", nullable = false)
    private boolean requiresStructuredOutput;

    @Column(name = "free_tier_approved", nullable = false)
    private boolean freeTierApproved;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @Column(name = "updated_by")
    private UUID updatedBy;
}
