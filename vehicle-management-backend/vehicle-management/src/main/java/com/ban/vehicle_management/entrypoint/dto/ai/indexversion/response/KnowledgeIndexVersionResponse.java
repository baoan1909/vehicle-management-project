package com.ban.vehicle_management.entrypoint.dto.ai.indexversion.response;

import java.time.Instant;

import com.ban.vehicle_management.shared.enumeration.ai.AiProvider;
import com.ban.vehicle_management.shared.enumeration.ai.KnowledgeIndexVersionStatus;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class KnowledgeIndexVersionResponse {
    private UUID indexVersionId;
    private String versionCode;
    private UUID modelConfigurationId;
    private AiProvider provider;
    private String modelId;
    private Integer dimension;
    private String chunkerVersion;
    private String embeddingPromptVersion;
    private String distanceMetric;
    private String normalization;
    private KnowledgeIndexVersionStatus status;
    private Integer expectedChunkCount;
    private Integer embeddedChunkCount;
    private Integer failedChunkCount;
    private String contentChecksum;
    private String failureCode;
    private Instant readyAt;
    private Instant activatedAt;
    private Instant retiredAt;
    private Instant createdAt;
    private Instant updatedAt;
}
