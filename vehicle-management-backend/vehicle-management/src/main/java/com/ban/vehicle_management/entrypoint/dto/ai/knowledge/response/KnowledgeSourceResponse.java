package com.ban.vehicle_management.entrypoint.dto.ai.knowledge.response;

import java.time.Instant;

import com.ban.vehicle_management.shared.enumeration.ai.KnowledgeAccessScope;
import com.ban.vehicle_management.shared.enumeration.ai.KnowledgeSourceStatus;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class KnowledgeSourceResponse {
    private UUID sourceId;
    private UUID tenantId;
    private String title;
    private String description;
    private KnowledgeAccessScope accessScope;
    private KnowledgeSourceStatus status;
    private Instant createdAt;
    private Instant updatedAt;
}
